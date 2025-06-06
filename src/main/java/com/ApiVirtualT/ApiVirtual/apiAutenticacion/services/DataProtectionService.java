package com.ApiVirtualT.ApiVirtual.apiAutenticacion.services;

import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class DataProtectionService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger logger = LoggerFactory.getLogger(DataProtectionService.class);

    public DataProtectionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ResponseEntity<Map<String, Object>> verificarEstadoTerminos(HttpServletRequest request) {
        try {
            // 1. Obtener identificación del cliente desde el token
            String clienteIdenti = (String) request.getAttribute("ClienIdenti");
            if (clienteIdenti == null || clienteIdenti.isEmpty()) {
                return errorResponse("Token inválido o sin información de cliente", HttpStatus.UNAUTHORIZED);
            }

            // 2. Obtener código de cliente desde la base de datos
            String clienteCodigo = obtenerCodigoCliente(clienteIdenti);
            if (clienteCodigo == null) {
                return errorResponse("Cliente no encontrado en el sistema", HttpStatus.NOT_FOUND);
            }

            // 3. Verificar estado de aceptación en la base de datos
            return verificarEstadoEnBaseDatos(clienteCodigo);

        } catch (Exception e) {
            logger.error("Error al verificar estado de términos: {}", e.getMessage(), e);
            return errorResponse("Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> registrarAceptacionTerminos(
            HttpServletRequest request, String estado) {
        try {
            // Validación del estado
            if (!"0".equals(estado) && !"1".equals(estado)) {
                return errorResponse("El estado debe ser '0' o '1'", HttpStatus.BAD_REQUEST);
            }

            // Obtener datos del cliente
            String clienteIdenti = (String) request.getAttribute("ClienIdenti");
            if (clienteIdenti == null) {
                return errorResponse("Token inválido o sin información de cliente", HttpStatus.UNAUTHORIZED);
            }

            Map<String, String> datosCliente = obtenerDatosCompletosCliente(clienteIdenti);
            if (datosCliente == null) {
                return errorResponse("Cliente no encontrado en el sistema", HttpStatus.NOT_FOUND);
            }

            String clienteCodigo = datosCliente.get("codigo");
            String usuarioId = datosCliente.get("usuario");
            String oficina = datosCliente.get("oficina");

            // Verificar si ya existe registro
            boolean existeRegistro = verificarExistenciaRegistro(clienteCodigo);

            // Registrar o actualizar según corresponda
            if (existeRegistro) {
                return actualizarRegistroTerminos(clienteCodigo, estado, usuarioId, oficina);
            } else {
                return crearNuevoRegistroTerminos(clienteCodigo, estado, usuarioId, oficina);
            }

        } catch (Exception e) {
            logger.error("Error al registrar aceptación de términos: {}", e.getMessage(), e);
            return errorResponse("Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Métodos auxiliares mejorados
    private String obtenerCodigoCliente(String clienteIdenti) {
        try {
            String sql = "SELECT clien_cod_clien FROM cnxclien WHERE clien_ide_clien = :id";
            Object result = entityManager.createNativeQuery(sql)
                    .setParameter("id", clienteIdenti)
                    .getSingleResult();
            return result.toString(); // Conversión segura a String
        } catch (NoResultException e) {
            logger.warn("No se encontró cliente con identificación: {}", clienteIdenti);
            return null;
        } catch (Exception e) {
            logger.error("Error al obtener código de cliente: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> obtenerDatosCompletosCliente(String clienteIdenti) {
        try {
            String sql = "SELECT clien_cod_clien, clien_cod_usuar, clien_cod_ofici " +
                    "FROM cnxclien WHERE clien_ide_clien = :id";

            Object[] resultado = (Object[]) entityManager.createNativeQuery(sql)
                    .setParameter("id", clienteIdenti)
                    .getSingleResult();

            Map<String, String> datos = new HashMap<>();
            datos.put("codigo", resultado[0].toString());
            datos.put("usuario", resultado[1].toString());
            datos.put("oficina", resultado[2].toString());

            return datos;
        } catch (Exception e) {
            logger.error("Error al obtener datos completos del cliente: {}", e.getMessage(), e);
            return null;
        }
    }

    private ResponseEntity<Map<String, Object>> verificarEstadoEnBaseDatos(String clienteCodigo) {
        try {
            String sql = "SELECT audlpdf_std_audlpdf FROM andaudlpdf WHERE audlpdf_cod_clien = :codigo";
            Object result = entityManager.createNativeQuery(sql)
                    .setParameter("codigo", clienteCodigo)
                    .getSingleResult();

            String estado = result.toString();

            if ("0".equals(estado)) {
                // Si el estado es "0" (no aceptado), devolver Bad Request
                return errorResponse("El usuario no ha aceptado los términos", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(crearSuccessResponse(
                    true,
                    "Términos ya aceptados",
                    estado
            ));

        } catch (NoResultException e) {
            // Cuando no hay registro, devolver Bad Request
            return errorResponse("No existe registro de consentimiento", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error al verificar estado en base de datos: {}", e.getMessage(), e);
            return errorResponse("Error al verificar estado de términos", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean verificarExistenciaRegistro(String clienteCodigo) {
        try {
            String sql = "SELECT * FROM andaudlpdf WHERE audlpdf_cod_clien = :codigo";
            entityManager.createNativeQuery(sql)
                    .setParameter("codigo", clienteCodigo)
                    .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private ResponseEntity<Map<String, Object>> actualizarRegistroTerminos(
            String clienteCodigo, String estado, String usuarioId, String oficina) {

        String sql = "UPDATE andaudlpdf SET " +
                "audlpdf_std_audlpdf = :estado, " +
                "audlpdf_fec_audlpdf = CURRENT, " +
                "audlpdf_cod_usuar = :usuario, " +
                "audlpdf_cod_ofici = :oficina " +
                "WHERE audlpdf_cod_clien = :codigo";

        int updated = entityManager.createNativeQuery(sql)
                .setParameter("estado", estado)
                .setParameter("usuario", usuarioId)
                .setParameter("oficina", oficina)
                .setParameter("codigo", clienteCodigo)
                .executeUpdate();

        if (updated > 0) {
            return ResponseEntity.ok(crearSuccessResponse(
                    true,
                    "Consentimiento actualizado exitosamente",
                    estado
            ));
        }
        return errorResponse("No se pudo actualizar el consentimiento", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> crearNuevoRegistroTerminos(
            String clienteCodigo, String estado, String usuarioId, String oficina) {

        String sql = "INSERT INTO andaudlpdf (" +
                "audlpdf_cod_canal, audlpdf_cod_clien, audlpdf_std_audlpdf, " +
                "audlpdf_dsmal_audlpdf, audlpdf_fec_audlpdf, " +
                "audlpdf_cod_usuar, audlpdf_cod_ofici) " +
                "VALUES ('3', :codigo, :estado, '1', CURRENT, :usuario, :oficina)";

        entityManager.createNativeQuery(sql)
                .setParameter("codigo", clienteCodigo)
                .setParameter("estado", estado)
                .setParameter("usuario", usuarioId)
                .setParameter("oficina", oficina)
                .executeUpdate();

        return ResponseEntity.ok(crearSuccessResponse(
                true,
                "Consentimiento registrado exitosamente",
                estado
        ));
    }

    private Map<String, Object> crearSuccessResponse(boolean tieneConsentimiento, String mensaje, String estado) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tieneConsentimiento", tieneConsentimiento);
        response.put("mensaje", mensaje);
        if (estado != null) {
            response.put("estado", estado);
        }
        return response;
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String mensaje, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", mensaje);
        response.put("status", status.value());
        return ResponseEntity.status(status).body(response);
    }
}