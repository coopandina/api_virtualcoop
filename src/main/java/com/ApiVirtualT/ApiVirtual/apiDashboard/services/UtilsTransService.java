package com.ApiVirtualT.ApiVirtual.apiDashboard.services;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.TokenExpirationService;
import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.InterbancariasDTO;
import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.VerMovimientoCta;
import com.ApiVirtualT.ApiVirtual.libs.Libs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;


@Transactional
@Service
@RequiredArgsConstructor
public class UtilsTransService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TokenExpirationService tokenExpirationService;
    int intentosRealizadoTokenFallos = 0;
    //UTILS TRANSFERENCIAS DIRECTAS
    public ResponseEntity<Map<String,Object>> inforUsuarios (HttpServletRequest token){
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> allDataList = new ArrayList<>();
        try{

            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            // Primera consulta: Obtener código de oficina y empresa
            String sqlObtenerCodigos = "SELECT clien_cod_ofici, clien_cod_empre " +
                    "FROM cnxclien " +
                    "WHERE clien_ide_clien = :clienIdenti " +
                    "AND clien_cod_clien = :numSocio";

            Query queryCodigos = entityManager.createNativeQuery(sqlObtenerCodigos);
            queryCodigos.setParameter("clienIdenti", clienIdenti);
            queryCodigos.setParameter("numSocio", numSocio);

            List<Object[]> resultadoCodigos = queryCodigos.getResultList();

            if (resultadoCodigos.isEmpty()) {
                response.put("message", "No se encontraron códigos de oficina y empresa para los parámetros proporcionados.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            // Extraer códigos de oficina y empresa
            Object[] codigos = resultadoCodigos.get(0);
            String codigoOficina = String.valueOf(codigos[0]); // Convertir a String
            String codigoEmpresa = String.valueOf(codigos[1]); // Convertir a String
            // Segunda consulta: Obtener datos con los códigos obtenidos
            String sqlDatosCuenta = "SELECT ctadp_cod_ctadp, ectad_des_ectad, depos_des_depos, ctadp_sal_dispo, ctadp_sal_nodis, ctadp_sal_ndchq " +
                    "FROM cnxctadp, cnxectad, cnxdepos " +
                    "WHERE ctadp_cod_empre = :codigoEmpresa " +
                    "AND ctadp_cod_ofici = :codigoOficina " +
                    "AND ctadp_cod_clien = :numSocio " +
                    "AND ctadp_cod_ectad <> '3' " +
                    "AND ctadp_cod_ectad = ectad_cod_ectad " +
                    "AND ctadp_cod_empre = depos_cod_empre " +
                    "AND ctadp_cod_ofici = depos_cod_ofici " +
                    "AND ctadp_cod_depos = depos_cod_depos";

            Query queryCuentas = entityManager.createNativeQuery(sqlDatosCuenta);
            queryCuentas.setParameter("codigoEmpresa", codigoEmpresa);
            queryCuentas.setParameter("codigoOficina", codigoOficina);
            queryCuentas.setParameter("numSocio", numSocio);
            List<Object[]> resultadosCuentas = queryCuentas.getResultList();
            if (resultadosCuentas.isEmpty()) {
                response.put("message", "No se encontraron datos de cuentas para los parámetros proporcionados.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            } else {
                for (Object[] row : resultadosCuentas) {
                    Map<String, Object> cuentaData = new HashMap<>();
                    cuentaData.put("ctadp_cod_ctadp", row[0].toString().trim());
                    cuentaData.put("ectad_des_ectad", row[1].toString().trim());
                    cuentaData.put("depos_des_depos", row[2].toString().trim());
                    cuentaData.put("ctadp_sal_dispo", row[3].toString().trim());
                    cuentaData.put("ctadp_sal_nodis", row[4].toString().trim());
                    cuentaData.put("ctadp_sal_ndchq", row[5].toString().trim());
                    cuentaData.put("status", "INFOUSEROK");
                    allDataList.add(cuentaData);
                }
                // Consulta para obtener total de créditos e inversiones
                String sqlTotalCreditosInversiones =
                        "SELECT " +
                                "(SELECT COUNT(*) " +
                                " FROM cnxcredi " +
                                " WHERE credi_cod_clien = :numSocio " +
                                " AND credi_cod_ecred != 5) AS total_creditos, " +
                                "(SELECT COUNT(*) " +
                                " FROM cnxinver " +
                                " WHERE inver_cod_clien = :numSocio " +
                                " AND inver_cod_einve IN (1, 2)) AS total_inversiones " +
                                "FROM systables " +
                                "WHERE tabid = 1";

                Query queryTotales = entityManager.createNativeQuery(sqlTotalCreditosInversiones);
                queryTotales.setParameter("numSocio", numSocio);

                List<Object[]> resultadosTotales = queryTotales.getResultList();
                if (!resultadosTotales.isEmpty()) {
                    Object[] totales = resultadosTotales.get(0);
                    Map<String, Object> totalesData = new HashMap<>();
                    totalesData.put("total_creditos", totales[0].toString());
                    totalesData.put("total_inversiones", totales[1].toString());
                    allDataList.add(totalesData);
                }
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        }catch (Exception e){
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> errorData = new HashMap<>();
            List<Map<String, Object>> errorList = new ArrayList<>();
            errorData.put("message", "Error interno del servidor");
            errorData.put("status", "ERROR001");
            errorData.put("errors", e.getMessage());
            errorList.add(errorData);
            errorResponse.put("AllData", errorList);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<Map<String, Object>> listarCtaBeneficiarios(HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            String sqlBeneficiarios =
                    "SELECT p.titular, p.descripcion, p.email, p.telefono_movil, p.id_persona, p.cta_banco, " +
                            "       CASE WHEN p.tipo_trf = 'E' THEN (SELECT ifspi_nom_ifspi FROM cnxifspi WHERE ifspi_cod_ifspi = p.id_banco) ELSE NULL END AS entidad_financiera, p.tipo_trf " +
                            "FROM personas_transferencias p " +
                            "WHERE p.id_persona = :numSocio " +
                            "AND p.vigente = 'T' " +
                            "AND (p.tipo_trf = 'I' OR (p.tipo_trf = 'E' AND p.tipo_prod_banc IN ('AH', 'CC')))";

            Query queryBeneficiarios = entityManager.createNativeQuery(sqlBeneficiarios);
            queryBeneficiarios.setParameter("numSocio", numSocio);
            List<Object[]> resultados = queryBeneficiarios.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron cuentas de beneficiarios asociadas a su cuenta.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            List<Map<String, Object>> beneficiariosList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> beneficiario = new HashMap<>();
                beneficiario.put("titular", row[0].toString().trim());
                beneficiario.put("descripcion", row[1].toString().trim());
                beneficiario.put("email", row[2].toString().trim());
                beneficiario.put("telefono_movil", row[3].toString().trim());
                beneficiario.put("id_persona", row[4].toString().trim());
                beneficiario.put("cta_banco", row[5].toString().trim());
                beneficiario.put("entidad_financiera", row[6] != null ? row[6].toString().trim() : "COAC ANDINA");
                beneficiario.put("tipo_trf", row[7].toString().trim());
                beneficiariosList.add(beneficiario);
            }
            // Ordenar la lista de beneficiarios por el nombre del titular (de A a Z)
            beneficiariosList.sort(Comparator.comparing(b -> b.get("titular").toString()));
            response.put("beneficiarios", beneficiariosList);
            response.put("status", "INFOUSEROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> listarCtaBenefDirectos(HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            String sql =
                    "SELECT p.titular, p.descripcion, p.email, p.telefono_movil, p.id_persona, p.cta_banco " +
                            "FROM personas_transferencias p " +
                            "WHERE p.id_persona = :numSocio " +
                            "AND p.tipo_trf = 'I' " +
                            "AND p.vigente = 'T'";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("numSocio", numSocio);
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron cuentas asociadas.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            List<Map<String, Object>> cuentasList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> cuenta = new HashMap<>();
                cuenta.put("titular", row[0] != null ? row[0].toString().trim() : null);
                cuenta.put("descripcion", row[1] != null ? row[1].toString().trim() : null);
                cuenta.put("email", row[2] != null ? row[2].toString().trim() : null);
                cuenta.put("telefono_movil", row[3] != null ? row[3].toString().trim() : null);
                cuenta.put("id_persona", row[4] != null ? row[4].toString().trim() : null);
                cuenta.put("cta_banco", row[5] != null ? row[5].toString().trim() : null);
                cuentasList.add(cuenta);
            }
            cuentasList.sort(Comparator.comparing(c -> c.get("titular").toString()));
            response.put("cuentas", cuentasList);
            response.put("status", "INFOUSEROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR001");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> eliminarBeneDirecto(HttpServletRequest token, VerMovimientoCta dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                response.put("message", "Token con informacion invalida, intente nuevamente.");
                response.put("status", "ERROR022");
                response.put("errors", "Error de token");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String cta_banco = dto.getCtadp_cod_ctadp();
            System.out.println(cta_banco);

            if (cta_banco == null || !cta_banco.matches("\\d{12}")) {
                response.put("message", "Número de cuenta inválido");
                response.put("status", "ERROR003");
                response.put("errors", "El número de cuenta debe contener exactamente 12 dígitos");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sql = """
                    SELECT titular,descripcion,email,telefono_movil,id_persona,cta_banco
                    	        FROM personas_transferencias WHERE id_persona= :numSocio
                                AND cta_banco= :cta_banco
                    			AND tipo_trf='I'
                    """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("numSocio", numSocio);
            query.setParameter("cta_banco",cta_banco);
            List<Object[]> resultados = query.getResultList();
            if (resultados.isEmpty()) {
                response.put("message", "No se encontro una cuenta asociada para poder eliminar.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            String sql1 = """
                    UPDATE personas_transferencias SET vigente='F' 
                    WHERE id_persona= :id_persona
                    AND cta_banco= :cta_banco
                    AND tipo_trf='I'
                    """;
            Query queryUpdate = entityManager.createNativeQuery(sql1);
            queryUpdate.setParameter("id_persona", numSocio);
            queryUpdate.setParameter("cta_banco", cta_banco);
            int rowsUpdated = queryUpdate.executeUpdate();
            if (rowsUpdated > 0) {
                response.put("message", "Actualización exitosa. Registros modificados: " + rowsUpdated);
                response.put("status", "SUCCESS");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", "No se encontró ningún registro para eliminar.");
                response.put("status", "NO_UPDATE");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR001");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> buscarPorNombreApellido(HttpServletRequest token, VerMovimientoCta dto ) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            String entradaBusqueda = dto.getNombreApellidosBus();

            // Validación para asegurarse de que la entrada no esté vacía ni nula
            if (entradaBusqueda == null || entradaBusqueda.trim().isEmpty()) {
                response.put("message", "La entrada de búsqueda no puede estar vacía.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            entradaBusqueda = entradaBusqueda.toUpperCase();

            // Validación para asegurarse de que la entrada solo contiene texto (letras y espacios)
            if (!entradaBusqueda.matches("[A-Za-z0-9ÁÉÍÓÚáéíóú\\s]+")) {
                response.put("message", "La entrada de búsqueda solo debe contener texto válido (letras, números y espacios).");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sqlBeneficiarios =
                    "SELECT p.titular, p.descripcion, p.email, p.telefono_movil, p.id_persona, p.cta_banco, " +
                            "       CASE WHEN p.tipo_trf = 'E' THEN (SELECT ifspi_nom_ifspi FROM cnxifspi WHERE ifspi_cod_ifspi = p.id_banco) ELSE NULL END AS entidad_financiera, p.tipo_trf " +
                            "FROM personas_transferencias p " +
                            "WHERE p.id_persona = :numSocio " +
                            "AND p.vigente = 'T' " +
                            "AND p.tipo_trf = 'I' AND p.tipo_prod_banc IN ('AH', 'CC') " +
                            "AND CONCAT(p.titular, p.cta_banco) LIKE :entradaBusqueda";
            Query queryBuscarBeneficiarios = entityManager.createNativeQuery(sqlBeneficiarios);
            queryBuscarBeneficiarios.setParameter("numSocio", numSocio);
            queryBuscarBeneficiarios.setParameter("entradaBusqueda", "%" + entradaBusqueda + "%");
            List<Object[]> resultados = queryBuscarBeneficiarios.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron beneficiarios con el nombre especificado.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            List<Map<String, Object>> beneficiariosList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> beneficiario = new HashMap<>();
                beneficiario.put("titular", row[0].toString().trim());
                beneficiario.put("descripcion", row[1].toString().trim());
                beneficiario.put("email", row[2].toString().trim());
                beneficiario.put("telefono_movil", row[3].toString().trim());
                beneficiario.put("id_persona", row[4].toString().trim());
                beneficiario.put("cta_banco", row[5].toString().trim());
                beneficiario.put("entidad_financiera", row[6] != null ? row[6].toString().trim() : "COAC ANDINA");
                beneficiario.put("tipo_trf", row[7].toString().trim());
                beneficiariosList.add(beneficiario);
            }

            // Ordenar la lista de beneficiarios por el nombre del titular (de A a Z)
            beneficiariosList.sort(Comparator.comparing(b -> b.get("titular").toString()));
            response.put("beneficiarios", beneficiariosList);
            response.put("status", "INFOUSEROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> validarBeneficiario(HttpServletRequest token, VerMovimientoCta dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            String numSocio = (String) token.getAttribute("numSocio");
            String numeroCuenta = dto.getCtadp_cod_ctadp();
            if (numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
                response.put("message", "El número de cuenta no puede estar vacío.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (numeroCuenta == null || !numeroCuenta.matches("\\d{12}")) {
                response.put("message", "Número de cuenta inválido");
                response.put("status", "ERROR003");
                response.put("errors", "El número de cuenta debe contener exactamente 12 dígitos");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Consulta SQL para buscar datos del beneficiario
            String sqlBeneficiarios =
                    "SELECT trim(clien_ape_clien) || ' ' || trim(clien_nom_clien) as nombre, " +
                            "ctadp_cod_ctadp, clien_dir_email, clien_tlf_celul, depos_des_depos " +
                            "FROM cnxclien, cnxctadp, cnxdepos " +
                            "WHERE ctadp_cod_ctadp = :numeroCuenta " +
                            "AND ctadp_cod_depos = 1 " +
                            "AND ctadp_cod_ectad = 1 " +
                            "AND clien_cod_clien = ctadp_cod_clien " +
                            "AND ctadp_cod_depos = depos_cod_depos " +
                            "AND ctadp_cod_ofici = depos_cod_ofici";

            // Ejecutar la consulta
            Query queryBuscarBeneficiarios = entityManager.createNativeQuery(sqlBeneficiarios);
            queryBuscarBeneficiarios.setParameter("numeroCuenta", numeroCuenta);
            List<Object[]> resultados = queryBuscarBeneficiarios.getResultList();

            // Verificar si se encontraron resultados
            if (resultados.isEmpty()) {
                response.put("message", "No existe el beneficiario o la cuenta está inactiva.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            // Mapear los resultados de la consulta
            Object[] row = resultados.get(0);
            String nombre = row[0].toString().trim();
            String clienDirEmail = row[2].toString().trim();
            String clienTlfCelul = row[3].toString().trim();
            String deposDesDepos = row[4].toString().trim();
            // Verificar si el beneficiario ya existe
            String sqlCheck = "SELECT * FROM personas_transferencias WHERE id_persona = :clienCodClien " +
                    "AND tipo_trf = 'I' AND cta_banco = :numeroCuenta AND vigente = 'T'";

            Query queryCheckBeneficiario = entityManager.createNativeQuery(sqlCheck);
            queryCheckBeneficiario.setParameter("clienCodClien", numSocio);
            queryCheckBeneficiario.setParameter("numeroCuenta", numeroCuenta);
            List<Object[]> resultadosCheck = queryCheckBeneficiario.getResultList();
            // Si ya existe, devolver un mensaje de error
            if (!resultadosCheck.isEmpty()) {
                response.put("message", "Ya existe un beneficiario registrado con los mismos datos.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Actualizar el estado del beneficiario a "vigente"
            String sqlUpdate = "UPDATE personas_transferencias SET vigente = 'T' WHERE id_persona = :clienCodClien " +
                    "AND tipo_trf = 'I' AND cta_banco = :numeroCuenta";

            Query queryUpdate = entityManager.createNativeQuery(sqlUpdate);
            queryUpdate.setParameter("clienCodClien", numSocio);
            queryUpdate.setParameter("numeroCuenta", numeroCuenta);
            queryUpdate.executeUpdate();

            Map<String, Object> beneficiario = new HashMap<>();
            beneficiario.put("nombre", nombre);
            beneficiario.put("cta_banco", numeroCuenta);
            beneficiario.put("descripcion", deposDesDepos);
            beneficiario.put("email", clienDirEmail);
            beneficiario.put("telefono_movil", clienTlfCelul);

            List<Map<String, Object>> beneficiariosList = new ArrayList<>();
            beneficiariosList.add(beneficiario);

            // Responder con la lista de beneficiarios
            response.put("beneficiarios", beneficiariosList);
            response.put("status", "INFOUSEROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> guardarBenefiDirecto(HttpServletRequest token, VerMovimientoCta dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            String numeroCuenta = dto.getCtadp_cod_ctadp();
            String benefiCorreo = dto.getCorreoBeneficiario();
            String estadoGuardar = dto.getEstadoGuardarBenefici();
            if (numeroCuenta == null || !numeroCuenta.matches("\\d{12}")) {
                response.put("message", "El número de cuenta debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (!"1".equals(estadoGuardar) && !"0".equals(estadoGuardar)) {
                response.put("message", "El estado solo puede ser '1' o '0'.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
//            if (benefiCorreo == null || !benefiCorreo.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
//                response.put("message", "El correo del beneficiario tiene una estructura inválida.");
//                response.put("status", "ERROR004");
//                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//            }
            // Validar estadoGuardar
            if (!"1".equals(estadoGuardar)) {
                response.put("message", "PASA A LA TRANSFERENCIA !.");
                response.put("status", "OK0050B");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            String sqlCheck = "SELECT * FROM personas_transferencias WHERE id_persona = :idPersona " +
                    "AND tipo_trf = 'I' AND cta_banco = :ctaBanco AND vigente  IN ('T','F') ";
            Query queryCheck = entityManager.createNativeQuery(sqlCheck);
            queryCheck.setParameter("idPersona", numSocio);
            queryCheck.setParameter("ctaBanco", numeroCuenta);
            List<Object> resultados = queryCheck.getResultList();

            if (!resultados.isEmpty()) {
                // Actualizar beneficiario existente
                String sqlUpdate = "UPDATE personas_transferencias SET email = :email, vigente = 'T' " +
                        "WHERE id_persona = :idPersona AND tipo_trf = 'I' AND cta_banco = :ctaBanco";
                Query queryUpdate = entityManager.createNativeQuery(sqlUpdate);
                queryUpdate.setParameter("email", benefiCorreo);
                queryUpdate.setParameter("idPersona", numSocio);
                queryUpdate.setParameter("ctaBanco", numeroCuenta);
                queryUpdate.executeUpdate();

                response.put("message", "Beneficiario actualizado exitosamente.");
                response.put("status", "GBOK001");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                String sqlBeneficiarios =
                        "SELECT trim(clien_ape_clien) || ' ' || trim(clien_nom_clien) as nombre, " +
                                "ctadp_cod_ctadp, clien_dir_email, clien_tlf_celul, depos_des_depos " +
                                "FROM cnxclien, cnxctadp, cnxdepos " +
                                "WHERE ctadp_cod_ctadp = :numeroCuenta " +
                                "AND ctadp_cod_depos = 1 " +
                                "AND ctadp_cod_ectad = 1 " +
                                "AND clien_cod_clien = ctadp_cod_clien " +
                                "AND ctadp_cod_depos = depos_cod_depos " +
                                "AND ctadp_cod_ofici = depos_cod_ofici";

                // Ejecutar la consulta
                Query queryBuscarBeneficiarios = entityManager.createNativeQuery(sqlBeneficiarios);
                queryBuscarBeneficiarios.setParameter("numeroCuenta", numeroCuenta);
                List<Object[]> resultados1 = queryBuscarBeneficiarios.getResultList();
                if (resultados1.isEmpty()) {
                    response.put("message", "No se encontraron beneficiarios con el nombre especificado.");
                    response.put("status", "ERROR002");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }

                for (Object[] row : resultados1) {
                    String titular =  row[0].toString().trim();
                    String numCuenta =  row[1].toString().trim();
                    String emailCta =  row[2].toString().trim();
                    String tlfMovilCta = row[3].toString().trim();
                    String desDepos =  row[4].toString().trim();
                    String descripcion = numCuenta + " - " + desDepos;

                    Libs fechaHoraService = new Libs(entityManager);
                    String fecha = fechaHoraService.obtenerFechaYHora();
                    System.out.println(fecha);

                // Insertar nuevo beneficiario
                String sqlInsert = "INSERT INTO personas_transferencias " +
                        "(id_persona, id_banco, cta_banco, tipo_prod_banc, titular, descripcion, tipo_trf, fecha_alta, " +
                        "user_name_oficial, cedula, tipo_identificacion, email, telefono_movil, vigente) VALUES " +
                        "(:idPersona, :idBanco, :ctaBanco, 'AH', :titular, :descripcion, 'I', :fechaAlta, :userName, " +
                        ":cedula, :tipoIdentificacion, :email, :telefonoMovil, 'T')";

                Query queryInsert = entityManager.createNativeQuery(sqlInsert);

                // Establecer los parámetros con valores adecuados, utilizando valores por defecto cuando sea necesario
                queryInsert.setParameter("idPersona", numSocio);
                queryInsert.setParameter("idBanco", "");
                queryInsert.setParameter("ctaBanco", numeroCuenta);
                queryInsert.setParameter("titular", titular);
                queryInsert.setParameter("descripcion", descripcion);
                queryInsert.setParameter("fechaAlta", fecha);
                queryInsert.setParameter("userName", clienIdenti);
                queryInsert.setParameter("cedula", "");
                queryInsert.setParameter("tipoIdentificacion", "");
                queryInsert.setParameter("email", benefiCorreo != null ? benefiCorreo : "");
                queryInsert.setParameter("telefonoMovil", tlfMovilCta);
                queryInsert.executeUpdate();
                response.put("message", "Beneficiario registrado exitosamente.");
                response.put("status", "GBOK002");
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public ResponseEntity<Map<String, Object>> verInfCuenta(HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();

        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            if (clienIdenti == null || clienIdenti.trim().isEmpty()) {
                response.put("message", "El identificador del cliente no está presente en el token.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sql = """
                    SELECT
                        ctadp.ctadp_cod_ctadp,
                        ectad.ectad_des_ectad,
                        depos.depos_des_depos,
                        ctadp.ctadp_sal_dispo,
                        ctadp.ctadp_sal_nodis
                    FROM
                        cnxctadp AS ctadp
                    INNER JOIN
                        cnxclien AS clien
                        ON ctadp.ctadp_cod_empre = clien.clien_cod_empre
                        AND ctadp.ctadp_cod_ofici = clien.clien_cod_ofici
                        AND ctadp.ctadp_cod_clien = clien.clien_cod_clien
                    INNER JOIN
                        cnxectad AS ectad
                        ON ctadp.ctadp_cod_ectad = ectad.ectad_cod_ectad
                    INNER JOIN
                        cnxdepos AS depos
                        ON ctadp.ctadp_cod_empre = depos.depos_cod_empre
                        AND ctadp.ctadp_cod_ofici = depos.depos_cod_ofici
                        AND ctadp.ctadp_cod_depos = depos.depos_cod_depos
                    WHERE
                        clien.clien_ide_clien = :clien_ide_clien
                        AND depos.depos_ctr_opera = 0
                        AND depos.depos_cod_moned = 2
                        AND ctadp.ctadp_cod_ectad <> '3'
                        AND ctadp.ctadp_cod_depos IN (1, 2, 6, 9)
                    ORDER BY
                        ctadp.ctadp_cod_depos;
        """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("clien_ide_clien", clienIdenti);

            @SuppressWarnings("unchecked")
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron cuentas asociadas.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            List<Map<String, Object>> cuentasList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> cuenta = new HashMap<>();
                String txtcodctadp = row[0].toString().trim();
                String txtEstaCuenta = row[1].toString().trim();
                String txtdesdepos = row[2].toString().trim();
                String saldoDisponible = row[3].toString().trim();
                String saldoNoDisponi = row[4].toString().trim();

                Double salDisForma = Double.parseDouble(saldoDisponible);
                Double saNoDisForma = Double.parseDouble(saldoNoDisponi);
                String saldoDisFormateado = formatMoneda(salDisForma);
                String saldoNoDisFormateado = formatMoneda(saNoDisForma);

                cuenta.put("cuenta", txtcodctadp);
                cuenta.put("estadoCta", txtEstaCuenta);
                cuenta.put("descripcion", txtdesdepos);
                cuenta.put("saldo_disponible", saldoDisFormateado);
                cuenta.put("salNoDisponibel", saldoNoDisFormateado);
                cuentasList.add(cuenta);
            }
            response.put("cuentas", cuentasList);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor.");
            response.put("status", "ERROR003");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> validarCtaTransEstado(HttpServletRequest token, VerMovimientoCta dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFecha();
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            if (clienIdenti == null || clienIdenti.trim().isEmpty()) {
                response.put("message", "El identificador del cliente no está presente en el token.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String estadoDevCtaTransferir =  dto.getEstadoGuardarBenefici();
            String ctadp_cod_ctadp = dto.getCtadp_cod_ctadp();
            if (estadoDevCtaTransferir == null || (!estadoDevCtaTransferir.equals("1") && !estadoDevCtaTransferir.equals("0"))) {
                response.put("message", "El estado debe ser 1 o 0.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (ctadp_cod_ctadp == null || !ctadp_cod_ctadp.matches("\\d{1,12}")) {
                response.put("message", "El código de cuenta debe ser un número de hasta 12 dígitos.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if(estadoDevCtaTransferir.equals("0")){
                String sqlVerInfo = """
                        SELECT * FROM cnxctadp, cnxclien
                        WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp
                        AND ctadp_cod_clien = :numSocio
                        AND clien_ide_clien = :clien_ide_clien
                        """;
                Query query1 = entityManager.createNativeQuery(sqlVerInfo);
                query1.setParameter("ctadp_cod_ctadp",ctadp_cod_ctadp);
                query1.setParameter("numSocio", numSocio);
                query1.setParameter("clien_ide_clien", clienIdenti);
                List<Object[]> resVerifiCta = query1.getResultList();
                if (resVerifiCta.isEmpty()) {
                    response.put("message", "La cuenta ingresada no corresponde o no corresponde a la informacion de la cuenta iniciada sesion.");
                    response.put("status", "ERROR008");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
                String sqlConsulta1 = """
                        SELECT
                            cta.ctadp_cod_ctadp,
                            dep.depos_des_depos,
                            cta.ctadp_sal_dispo,
                            cta.ctadp_sal_nodis,
                            cli.clien_nom_clien,
                            cli.clien_ape_clien
                        FROM
                            cnxclien AS cli
                        INNER JOIN
                            cnxctadp AS cta
                            ON cli.clien_cod_empre = cta.ctadp_cod_empre
                            AND cli.clien_cod_ofici = cta.ctadp_cod_ofici
                            AND cli.clien_cod_clien = cta.ctadp_cod_clien
                        INNER JOIN
                            cnxdepos AS dep
                            ON cta.ctadp_cod_empre = dep.depos_cod_empre
                            AND cta.ctadp_cod_ofici = dep.depos_cod_ofici
                            AND cta.ctadp_cod_depos = dep.depos_cod_depos
                        INNER JOIN
                            cnxopdep AS opd
                            ON cta.ctadp_cod_empre = opd.opdep_cod_empre
                            AND cta.ctadp_cod_ofici = opd.opdep_cod_ofici
                            AND cta.ctadp_cod_depos = opd.opdep_cod_depos
                            AND cta.ctadp_cod_ectad = opd.opdep_cod_ectad
                        WHERE
                            cli.clien_ide_clien = :clien_ide_clien
                            AND dep.depos_ctr_opera = 0
                            AND dep.depos_cod_moned = 2
                            AND opd.opdep_cod_toper = '3'
                            AND dep.depos_cod_depos IN (1,9)
                            AND cta.ctadp_cod_ctadp <> :ctadp_cod_ctadp
                        ORDER BY
                            cta.ctadp_cod_depos;
                        """;
                Query query = entityManager.createNativeQuery(sqlConsulta1);
                query.setParameter("clien_ide_clien", clienIdenti);
                query.setParameter("ctadp_cod_ctadp",ctadp_cod_ctadp);
                @SuppressWarnings("unchecked")
                List<Object[]> resultados = query.getResultList();

                if (resultados.isEmpty()) {
                    response.put("message", "No se encontraron cuentas asociadas.");
                    response.put("status", "ERROR456");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
                List<Map<String, Object>> cuentasList = new ArrayList<>();
                for (Object[] row : resultados) {
                    Map<String, Object> cuenta = new HashMap<>();
                    String txtcodctadp = row[0].toString().trim();
                    String txtdesdepos = row[1].toString().trim();
                    String saldoDisponible = row[2].toString().trim();
                    String saldoNoDisponi = row[3].toString().trim();
                    String nombProCta = row[4].toString().trim();
                    String apelProCta = row[5].toString().trim();
                    Double salDisForma = Double.parseDouble(saldoDisponible);
                    Double saNoDisForma = Double.parseDouble(saldoNoDisponi);
                    String saldoDisFormateado = formatMoneda(salDisForma);
                    String saldoNoDisFormateado = formatMoneda(saNoDisForma);


                    cuenta.put("cuenta", txtcodctadp);
                    cuenta.put("descripcion", txtdesdepos);
                    cuenta.put("saldo_disponible", saldoDisFormateado);
                    cuenta.put("salNoDisponibel", saldoNoDisFormateado);
                    cuenta.put("apellidos", apelProCta);
                    cuenta.put("nombres", nombProCta);
                    cuenta.put("fecha", fecha);
                    cuentasList.add(cuenta);
                }
                response.put("cuentas", cuentasList);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }else{
                String sql = """
                        SELECT
                        ctadp.ctadp_cod_ctadp,
                        ectad.ectad_des_ectad,
                        depos.depos_des_depos,
                        ctadp.ctadp_sal_dispo,
                        ctadp.ctadp_sal_nodis,
                        clien.clien_nom_clien,
                        clien.clien_ape_clien
                    FROM
                        cnxctadp AS ctadp
                    INNER JOIN
                        cnxclien AS clien
                        ON ctadp.ctadp_cod_empre = clien.clien_cod_empre
                        AND ctadp.ctadp_cod_ofici = clien.clien_cod_ofici
                        AND ctadp.ctadp_cod_clien = clien.clien_cod_clien
                    INNER JOIN
                        cnxectad AS ectad
                        ON ctadp.ctadp_cod_ectad = ectad.ectad_cod_ectad
                    INNER JOIN
                        cnxdepos AS depos
                        ON ctadp.ctadp_cod_empre = depos.depos_cod_empre
                        AND ctadp.ctadp_cod_ofici = depos.depos_cod_ofici
                        AND ctadp.ctadp_cod_depos = depos.depos_cod_depos
                    WHERE
                        clien.clien_ide_clien = :clien_ide_clien
                        AND depos.depos_ctr_opera = 0
                        AND depos.depos_cod_moned = 2
                        AND ctadp.ctadp_cod_ectad <> '3'
                        AND ctadp.ctadp_cod_ctadp <> :ctadp_cod_ctadp
                        AND ctadp.ctadp_cod_depos IN (1)
                    ORDER BY
                        ctadp.ctadp_cod_depos;
        """;
                Query query = entityManager.createNativeQuery(sql);
                query.setParameter("clien_ide_clien", clienIdenti);
                query.setParameter("ctadp_cod_ctadp", ctadp_cod_ctadp);

                @SuppressWarnings("unchecked")
                List<Object[]> resultados = query.getResultList();

                if (resultados.isEmpty()) {
                    response.put("message", "No se puede transferir a su propia cuenta de ahorros. ");
                    response.put("status", "ERROR014");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
                List<Map<String, Object>> cuentasList = new ArrayList<>();
                for (Object[] row : resultados) {
                    Map<String, Object> cuenta = new HashMap<>();
                    String txtcodctadp = row[0].toString().trim();
                    String txtEstaCuenta = row[1].toString().trim();
                    String txtdesdepos = row[2].toString().trim();
                    String saldoDisponible = row[3].toString().trim();
                    String saldoNoDisponi = row[4].toString().trim();
                    String nombProCta = row[5].toString().trim();
                    String apelProCta = row[6].toString().trim();

                    Double salDisForma = Double.parseDouble(saldoDisponible);
                    Double saNoDisForma = Double.parseDouble(saldoNoDisponi);
                    String saldoDisFormateado = formatMoneda(salDisForma);
                    String saldoNoDisFormateado = formatMoneda(saNoDisForma);

                    cuenta.put("cuenta", txtcodctadp);
                    cuenta.put("estadoCta", txtEstaCuenta);
                    cuenta.put("descripcion", txtdesdepos);
                    cuenta.put("saldo_disponible", saldoDisFormateado);
                    cuenta.put("salNoDisponibel", saldoNoDisFormateado);
                    cuenta.put("apellidos", apelProCta);
                    cuenta.put("nombres", nombProCta);
                    cuenta.put("fecha", fecha);
                    cuentasList.add(cuenta);
                }
                response.put("cuentas", cuentasList);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

        } catch (Exception e) {
            response.put("message", "Error interno del servidor.");
            response.put("status", "ERROR333");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //UTILS TRANSFERENCIAS INTERBANCARIAS

    public ResponseEntity<Map<String, Object>>lisCtaTransferibles(HttpServletRequest token){
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> allData = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                List<Map<String, Object>> allDataList =  new ArrayList<>();
                allData.put("message", "Datos del token incompletos");
                allData.put("status", "ERRORTRFINTER001");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            String sqlListCtaTransferibles = "SELECT ctadp_cod_depos,ctadp_cod_ctadp,depos_des_depos,ctadp_cod_ectad " +
                    "FROM cnxclien,cnxctadp,cnxdepos,cnxopdep " +
                    "WHERE clien_ide_clien= :clien_ide_clien " +
                    "AND ctadp_cod_empre=clien_cod_empre " +
                    "AND ctadp_cod_ofici=clien_cod_ofici " +
                    "AND ctadp_cod_clien=clien_cod_clien " +
                    "AND ctadp_cod_depos=1 " +
                    "AND depos_cod_empre=ctadp_cod_empre " +
                    "AND depos_cod_ofici=ctadp_cod_ofici " +
                    "AND depos_cod_depos=ctadp_cod_depos " +
                    "AND depos_ctr_opera=0 " +
                    "AND depos_cod_moned=2 " +
                    "AND opdep_cod_empre=ctadp_cod_empre " +
                    "AND opdep_cod_ofici=ctadp_cod_ofici " +
                    "AND opdep_cod_depos=ctadp_cod_depos " +
                    "AND opdep_cod_ectad=ctadp_cod_ectad " +
                    "AND opdep_cod_toper = 3 " +
                    "ORDER BY depos_cod_depos ";
            Query queryListCtaTransferibles = entityManager.createNativeQuery(sqlListCtaTransferibles);
            queryListCtaTransferibles.setParameter("clien_ide_clien", clienIdenti);
            List<Object[]> listCta = queryListCtaTransferibles.getResultList();

            if(listCta.isEmpty()){
                response.put("message", "No posee cuentas disponibles para transfererir");
                response.put("status", "ERRORTRFINTER002");
                response.put("error", "No se encontraron cuentas para transferir en la bdd");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            List<Map<String, Object>> cuentas = new ArrayList<>();
            for(Object [] row : listCta){
                String codDepos = row[0].toString().trim();
                String numCta = row[1].toString().trim();
                String descrCta = row[2].toString().trim();
                String ectaCta = row[3].toString().trim();
                String saldoCta = obtenerSaldoDisponible(numCta);
                Double saldoDouble = Double.parseDouble(saldoCta);
                String saldoTransfor = formatMoneda(saldoDouble);
                Map<String, Object> cuenta =  new HashMap<>();
                cuenta.put("codigoCta", codDepos);
                cuenta.put("numeroCta", numCta);
                cuenta.put("descrCta", descrCta);
                cuenta.put("estadCta", ectaCta);
                cuenta.put("saldoCta", "$" + saldoTransfor);
                cuentas.add(cuenta);
            }
            response.put("Cuentas Transferibles", cuentas);
            return new ResponseEntity<>(response, HttpStatus.OK);
            } catch (Exception e) {
                response.put("message", "Error interno del servidor.");
                response.put("status", "ERROR003");
                response.put("errors", e.getMessage());
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
    }
    public ResponseEntity<Map<String, Object>>lisBeneInterbanc(HttpServletRequest token){
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> allData = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                List<Map<String, Object>> allDataList =  new ArrayList<>();
                allData.put("message", "Datos del token incompletos");
                allData.put("status", "ERRORTRFINTER001");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            String sqlListBeneficiariosExternos =
                    "SELECT p.titular, " +
                            "       p.descripcion, " +
                            "       p.email, " +
                            "       p.telefono_movil, " +
                            "       p.id_persona, " +
                            "       p.tipo_identificacion, "  +
                            "       p.cedula, " +
                            "       p.tipo_prod_banc, " +
                            "       p.cta_banco, " +
                            "       CASE " +
                            "           WHEN p.tipo_trf = 'E' THEN " +
                            "               (SELECT ifspi_nom_ifspi FROM cnxifspi WHERE ifspi_cod_ifspi = p.id_banco) " +
                            "           ELSE NULL " +
                            "       END AS entidad_financiera, " +
                            "       p.tipo_trf " +
                            "FROM personas_transferencias p " +
                            "WHERE p.id_persona = :numSocio "+
                            "  AND p.vigente = 'T' " +
                            "  AND p.tipo_trf = 'E' " +
                            "  AND p.tipo_prod_banc IN ('AH', 'CC')";

            Query queryListBeneExternos = entityManager.createNativeQuery(sqlListBeneficiariosExternos);
            queryListBeneExternos.setParameter("numSocio", numSocio);
            List<Object[]> resulBeneExternos = queryListBeneExternos.getResultList();
            if(resulBeneExternos.isEmpty()){
                response.put("message", "No posee beneficiarios externos asociadas a su cuenta");
                response.put("status", "ERRORTRFINTER002");
                response.put("error", "No se encontraron cuentas para transferir en la bdd");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            List<Map<String, Object>> beneficiarios = new ArrayList<>();
            for(Object [] row : resulBeneExternos){
                String titularCta = row[0].toString().trim();
                String descripcionCta = row[1].toString().trim();
                String emailCta = row[2].toString().trim();
                String tlfCta = row[3].toString().trim();
                String idPersona = row[4].toString().trim();
                String tipoIdentifi = row[5].toString().trim();
                String numidentificaciCta = row[6].toString().trim();
                String tipoProdBanco = row[7].toString().trim();
                String ctaBanco = row[8].toString().trim();
                String entidadFinaCta = row[9].toString().trim();
                String tipoCta = row[10].toString().trim();

                Map<String, Object> beneficiario =  new HashMap<>();
                beneficiario.put("titular", titularCta);
                beneficiario.put("descripcion", descripcionCta);
                beneficiario.put("email", emailCta);
                beneficiario.put("telefono", tlfCta);
                beneficiario.put("idPersona",idPersona);
                beneficiario.put("tipoIdentificacion", tipoIdentifi);
                beneficiario.put("numIdentificacion", numidentificaciCta);
                beneficiario.put("tipo prodcuto", tipoProdBanco);
                beneficiario.put("cuenta Banco", ctaBanco);
                beneficiario.put("entidad financiera", entidadFinaCta);
                beneficiario.put("tipoCta", tipoCta);
                beneficiarios.add(beneficiario);
            }
            beneficiarios.sort(Comparator.comparing(b -> b.get("titular").toString()));
            response.put("Beneficiarios Interbancarios", beneficiarios);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("message", "Error interno del servidor.");
            response.put("status", "ERROR003");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> buscarCuentaInterbancaria(HttpServletRequest token, VerMovimientoCta dto ) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            String entradaBusqueda = dto.getNombreApellidosBus();

            if (entradaBusqueda == null || entradaBusqueda.trim().isEmpty()) {
                response.put("message", "La entrada de búsqueda no puede estar vacía.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            entradaBusqueda = entradaBusqueda.toUpperCase();
            // Verificar que la entrada contenga solo letras, números y espacios
            if (!entradaBusqueda.matches("[A-Za-z0-9ÁÉÍÓÚáéíóú\\s]+")) {
                response.put("message", "La entrada de búsqueda solo debe contener texto válido (letras, números y espacios).");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sqlBeneficiarios =
                    "SELECT p.titular, p.descripcion, p.email, p.telefono_movil, p.id_persona, p.cta_banco, " +
                            "       CASE WHEN p.tipo_trf = 'E' THEN (SELECT ifspi_nom_ifspi FROM cnxifspi WHERE ifspi_cod_ifspi = p.id_banco) ELSE NULL END AS entidad_financiera, p.tipo_trf " +
                            "FROM personas_transferencias p " +
                            "WHERE p.id_persona = :numSocio " +
                            "AND p.vigente = 'T' " +
                            "AND p.tipo_trf = 'E' " +
                            "AND p.tipo_prod_banc IN ('AH', 'CC') " +
                            "AND CONCAT(p.titular, p.cta_banco) LIKE :entradaBusqueda";
            Query queryBuscarBeneficiarios = entityManager.createNativeQuery(sqlBeneficiarios);
            queryBuscarBeneficiarios.setParameter("numSocio", numSocio);
            queryBuscarBeneficiarios.setParameter("entradaBusqueda", "%" + entradaBusqueda + "%");
            List<Object[]> resultados = queryBuscarBeneficiarios.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron beneficiarios con el nombre especificado.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            List<Map<String, Object>> beneficiariosList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> beneficiario = new HashMap<>();
                beneficiario.put("titular", row[0].toString().trim());
                beneficiario.put("descripcion", row[1].toString().trim());
                beneficiario.put("email", row[2].toString().trim());
                beneficiario.put("telefono_movil", row[3].toString().trim());
                beneficiario.put("id_persona", row[4].toString().trim());
                beneficiario.put("cta_banco", row[5].toString().trim());
                beneficiario.put("entidad_financiera", row[6] != null ? row[6].toString().trim() : "COAC ANDINA");
                beneficiario.put("tipo_trf", row[7].toString().trim());
                beneficiariosList.add(beneficiario);
            }

            // Ordenar la lista de beneficiarios por el nombre del titular (de A a Z)
            beneficiariosList.sort(Comparator.comparing(b -> b.get("titular").toString()));
            response.put("beneficiarios", beneficiariosList);
            response.put("status", "INFOUSEROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR003");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> listarInstFinancieras(HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();
        Map<String,Object>allData = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                List<Map<String, Object>> allDataList =  new ArrayList<>();
                allData.put("message", "Datos del token incompletos");
                allData.put("status", "ERRORTRFINTER001");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String sqlInstiFinancieras =
                    "SELECT ifspi_cod_ifspi, ifspi_nom_ifspi FROM cnxifspi WHERE ifspi_bce_ctaco IS NOT NULL " +
                    "AND length(ifspi_bce_ctaco) > 0 " +
                    "AND ifspi_cod_ifspi NOT IN (3) ORDER BY ifspi_cod_ifspi";
            Query queryIntituFinan = entityManager.createNativeQuery(sqlInstiFinancieras);
            List<Object[]> resultados = queryIntituFinan.getResultList();
            if (resultados.isEmpty()) {
                response.put("message", "No se encontrar instituciciones financieras en la BDD.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            List<Map<String, Object>> institucionesList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> institucion = new HashMap<>();
                institucion.put("codigo", row[0].toString().trim());
                institucion.put("nombreInstitucion", row[1].toString().trim());
                institucionesList.add(institucion);
            }
            response.put("Instituciones", institucionesList);
            response.put("status", "INFOUSEROK");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> guardarBeneInterbancario(HttpServletRequest request, InterbancariasDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) request.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) request.getAttribute("ClienIdenti");
            String numSocio = (String) request.getAttribute("numSocio");

            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                response.put("message", "Datos del token incompletos");
                response.put("status", "ERRORTRFINTER001");
                response.put("errors", "ERROR EN LA AUTENTICACIÓN");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String numCuenta = dto.getNumCuenta();
            String tipoCuenta= dto.getTipoCuenta();
            String estadoGuardar = dto.getEstadoGuardarBenefici();
            String benefiCorreo = dto.getCorreoElectronico();
            String movilInter = dto.getNumCelular();
            String nombreTitular = dto.getNombreTitularCta();
            String codInstitucion = dto.getCodInstitucion();
            String numIdentificacion = dto.getNumIdentifiacion();
            String tipoIdentificacion = dto.getTipIdentiCta();
            // Validaciones de datos
            if (movilInter == null || movilInter.isEmpty() || !movilInter.matches("\\d+")) {
                response.put("message", "El número de celular solo puede contener números y no puede estar vacío o nulo.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (nombreTitular == null || nombreTitular.isEmpty() || nombreTitular.length() > 250 || !nombreTitular.matches("^[a-zA-Z\\s]+$")) {
                response.put("message", "El nombre del titular solo puede contener letras y espacios, no puede exceder los 250 caracteres, y no puede estar vacío o nulo.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (codInstitucion == null || codInstitucion.isEmpty() || !codInstitucion.matches("\\d+")) {
                response.put("message", "El código de institución solo puede contener números y no puede estar vacío o nulo.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (numIdentificacion == null || numIdentificacion.isEmpty() || !numIdentificacion.matches("\\d+")) {
                response.put("message", "El número de identificación solo puede contener números y no puede estar vacío o nulo.");
                response.put("status", "ERROR004");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (tipoIdentificacion == null || tipoIdentificacion.isEmpty() || !tipoIdentificacion.matches("^[a-zA-Z]+$")) {
                response.put("message", "El tipo de identificación solo puede contener letras y no puede estar vacío o nulo.");
                response.put("status", "ERROR005");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validaciones de datos
            if (numCuenta == null ) {
                response.put("message", "El número de cuenta no puede estar vacio o null.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (!"1".equals(estadoGuardar) && !"0".equals(estadoGuardar)) {
                response.put("message", "El estado solo puede ser '1' o '0'.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (benefiCorreo == null || !benefiCorreo.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                response.put("message", "El correo del beneficiario tiene una estructura inválida.");
                response.put("status", "ERROR004");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (!"1".equals(estadoGuardar)) {
                response.put("message", "El estado Guardar no permite realizar esta operación.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFechaYHora();
            System.out.println(fecha);

            // Verificar si ya existe un beneficiario
            String sqlCheck = "SELECT * FROM personas_transferencias WHERE id_persona = :clienCodClien AND cta_banco = :ctaBanco AND tipo_trf = 'E' AND vigente = 'T'";
            Query queryCheck = entityManager.createNativeQuery(sqlCheck);
            queryCheck.setParameter("clienCodClien", numSocio);
            queryCheck.setParameter("ctaBanco", numCuenta);
            List<Object> resultadosCheck = queryCheck.getResultList();
            if (!resultadosCheck.isEmpty()) {
                response.put("message", "Ya existe un beneficiario con los mismos datos.");
                response.put("status", "ERROR007");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Si no existe, verificar si hay uno con estado 'F'
            String sqlCheckInactive = "SELECT * FROM personas_transferencias WHERE id_persona = :clienCodClien AND cta_banco = :ctaBanco AND tipo_trf = 'E' AND vigente = 'F'";
            Query queryCheckInactive = entityManager.createNativeQuery(sqlCheckInactive);
            queryCheckInactive.setParameter("clienCodClien", numSocio);
            queryCheckInactive.setParameter("ctaBanco", numCuenta);
            List<Object> resultadosInactive = queryCheckInactive.getResultList();
            if (!resultadosInactive.isEmpty()) {
                String sqlUpdate = "UPDATE personas_transferencias SET email = :email, telefono_movil = :movil, vigente = 'T' WHERE id_persona = :clienCodClien AND cta_banco = :ctaBanco AND tipo_trf = 'E' AND vigente = 'F'";
                Query queryUpdate = entityManager.createNativeQuery(sqlUpdate);
                queryUpdate.setParameter("email", benefiCorreo);
                queryUpdate.setParameter("movil", movilInter);
                queryUpdate.setParameter("clienCodClien", numSocio);
                queryUpdate.setParameter("ctaBanco", numCuenta);
                queryUpdate.executeUpdate();
                response.put("message", "Beneficiario actualizado exitosamente.");
                response.put("status", "GBOK001");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            // Insertar un nuevo beneficiario
            String sqlInsert = "INSERT INTO personas_transferencias (id_persona, id_banco, cta_banco, tipo_prod_banc, titular, descripcion, tipo_trf, fecha_alta, user_name_oficial, cedula, tipo_identificacion, email, telefono_movil, vigente) " +
                    "VALUES (:clienCodClien, :insBeneInter, :ctaBanco, :tipoCuenta , :nomTitular, :benefiDetalle, 'E', :fechaAlta, :userName, :ideBeneficiario, :tipIden, :email, :movil, 'T')";
            Query queryInsertBeneficiario = entityManager.createNativeQuery(sqlInsert);
            queryInsertBeneficiario.setParameter("tipoCuenta", tipoCuenta);
            queryInsertBeneficiario.setParameter("clienCodClien", numSocio);
            queryInsertBeneficiario.setParameter("insBeneInter", codInstitucion);
            queryInsertBeneficiario.setParameter("ctaBanco", numCuenta);
            queryInsertBeneficiario.setParameter("nomTitular", nombreTitular);
            queryInsertBeneficiario.setParameter("benefiDetalle", numCuenta + " - " + nombreTitular);
            queryInsertBeneficiario.setParameter("fechaAlta", fecha);
            queryInsertBeneficiario.setParameter("userName", clienIdenti);
            queryInsertBeneficiario.setParameter("ideBeneficiario", numIdentificacion);
            queryInsertBeneficiario.setParameter("tipIden", tipoIdentificacion);
            queryInsertBeneficiario.setParameter("email", benefiCorreo);
            queryInsertBeneficiario.setParameter("movil", movilInter);
            queryInsertBeneficiario.executeUpdate();
            response.put("message", "Beneficiario registrado exitosamente.");
            response.put("status", "GBOK002");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> buscarInstiFinanciera(HttpServletRequest request, VerMovimientoCta dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) request.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) request.getAttribute("ClienIdenti");
            String numSocio = (String) request.getAttribute("numSocio");
            String nombreEntidadBusqueda = dto.getNombreApellidosBus();
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                response.put("message", "Datos del token incompletos");
                response.put("status", "ERRORTRFINTER001");
                response.put("errors", "ERROR EN LA AUTENTICACIÓN");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (nombreEntidadBusqueda == null || nombreEntidadBusqueda.isEmpty() || nombreEntidadBusqueda.length() > 250 || !nombreEntidadBusqueda.matches("^[a-zA-Z\\s]+$")) {
                response.put("message", "El nombre de la institucion financiera solo puede contener letras y espacios, no puede exceder los 250 caracteres, y no puede estar vacío o nulo.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFechaYHora();
            System.out.println(fecha);
            String sqlBuscarInstiFinan = "SELECT ifspi_cod_ifspi, ifspi_nom_ifspi FROM cnxifspi WHERE ifspi_bce_ctaco IS NOT NULL " +
                    "AND length(ifspi_bce_ctaco) > 0 " +
                    "AND ifspi_cod_ifspi NOT IN (3) " +
                    "AND ifspi_nom_ifspi LIKE :nombreEntidadBusqueda " +
                    "ORDER BY ifspi_cod_ifspi ";
            Query queryBusquedaInstitucion = entityManager.createNativeQuery(sqlBuscarInstiFinan);
            queryBusquedaInstitucion.setParameter("nombreEntidadBusqueda", "%"+ nombreEntidadBusqueda + "%");
            List<Object[]> resultado = queryBusquedaInstitucion.getResultList();
            if (resultado.isEmpty()) {
                response.put("message", "No se encontraro una institucion financiera que corresponda con el nombre especificado.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            List<Map<String, Object>> institucionesList  = new ArrayList<>();
            for (Object[] row : resultado){
                Map<String, Object> institucion = new HashMap<>();
                String codEntidadFina = row[0].toString().trim();
                String nombEntidadFinan = row[1].toString().trim();
                institucion.put("codigo", codEntidadFina);
                institucion.put("nombreInstitu", nombEntidadFinan);
                institucionesList.add(institucion);
            }
            response.put("message", "Institucion encontrada exitosamente.");
            response.put("instituciones", institucionesList);
            response.put("status", "INSOK002");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String obtenerSaldoDisponible(String txtcodctadp) throws Exception {
        try {

            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFecha();
            System.out.println(fecha);


            String sqlSaldoDisponible = "CALL cnxprc_sldos_ctadp(:codigoCuenta, :fecha)";
            Query querySaldo = entityManager.createNativeQuery(sqlSaldoDisponible);
            querySaldo.setParameter("codigoCuenta", txtcodctadp);
            querySaldo.setParameter("fecha", fecha);
            List<Object[]> resultadoSaldo = querySaldo.getResultList();

            if (resultadoSaldo.isEmpty()) {
                throw new Exception("No se pudo obtener el saldo disponible.");
            }
            return resultadoSaldo.get(0)[0].toString().trim();
        } catch (Exception e) {
            throw new Exception("Error al obtener el saldo disponible: " + e.getMessage(), e);
        }
    }
    public String verInfRetMax(String txtcodctadp) throws Exception {
        try {
            String sql = "SELECT ctart_max_mnret FROM cnxctart WHERE ctart_cod_ctadp = :codigoCuenta";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("codigoCuenta", txtcodctadp);
            List<Object> resultado = query.getResultList();

            if (resultado.isEmpty()) {
                return formatMoneda(0);
            }
            Object maxMnRet = resultado.get(0);
            if (maxMnRet == null) {
                return formatMoneda(0);
            }
            // Si el valor es de tipo BigDecimal, lo convertimos a double
            if (maxMnRet instanceof BigDecimal) {
                return formatMoneda(((BigDecimal) maxMnRet).doubleValue());
            }
            // Si no es un BigDecimal, lo intentamos convertir directamente a Double
            return formatMoneda(Double.parseDouble(maxMnRet.toString().trim()));

        } catch (Exception e) {
            throw new Exception("Error al obtener la información máxima de retiro: " + e.getMessage(), e);
        }
    }

    public ResponseEntity<Map<String, Object>> obtenerMovimientos(VerMovimientoCta dto, HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();
        try {
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            if (clienIdenti == null || clienIdenti.trim().isEmpty()) {
                response.put("message", "El identificador del cliente no está presente en el token.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (numSocio == null || numSocio.trim().isEmpty()) {
                response.put("message", "El usario del cliente no está presente en el token.");
                response.put("status", "ERROR047");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validar y convertir las fechas proporcionadas en el DTO
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date fechaDesde = dateFormat.parse(dto.getFechaDesdeCons());
            Date fechaHasta = dateFormat.parse(dto.getFechaHastaCons());

            // Calcular la diferencia en meses entre fechaDesde y fechaHasta
            Calendar calendarDesde = Calendar.getInstance();
            calendarDesde.setTime(fechaDesde);

            Calendar calendarHasta = Calendar.getInstance();
            calendarHasta.setTime(fechaHasta);

            int diffMeses = calendarHasta.get(Calendar.MONTH) - calendarDesde.get(Calendar.MONTH) +
                    (calendarHasta.get(Calendar.YEAR) - calendarDesde.get(Calendar.YEAR)) * 12;

            if (diffMeses > 3) {
                response.put("message", "El periodo de búsqueda no puede ser mayor a 3 meses.");
                response.put("status", "ERROR005");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(fechaHasta);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            fechaHasta = calendar.getTime();

            calendar.setTime(fechaDesde);
            int anoDesde = calendar.get(Calendar.YEAR);

            // Obtener oficina y empresa del cliente
            String sqlOfiEmpre = "SELECT clien_cod_ofici, clien_cod_empre FROM cnxclien " +
                    "WHERE clien_ide_clien = :clien_ide_clien AND clien_cod_clien = :clien_cod_clien";
            Query valQuery = entityManager.createNativeQuery(sqlOfiEmpre);
            valQuery.setParameter("clien_ide_clien", clienIdenti);
            valQuery.setParameter("clien_cod_clien", numSocio);
            List<Object[]> datosSocio = valQuery.getResultList();

            String oficina = "";
            String empresa = "";

            for (Object[] datos : datosSocio) {
                oficina = datos[0].toString().trim();
                empresa = datos[1].toString().trim();
            }

            if (oficina.isEmpty() || empresa.isEmpty()) {
                response.put("message", "No se encontraron datos de oficina o empresa.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // Consulta de movimientos
            String sqlDetalleMovimientos = "CALL detalle_movi1(:empresa, :oficina, :cuenta, :anoDesde)";
            Query detalleQuery = entityManager.createNativeQuery(sqlDetalleMovimientos);
            detalleQuery.setParameter("empresa", empresa);
            detalleQuery.setParameter("oficina", oficina);
            detalleQuery.setParameter("cuenta", dto.getCtadp_cod_ctadp());
            detalleQuery.setParameter("anoDesde", anoDesde);
            List<Object[]> registros = detalleQuery.getResultList();

            if (registros.isEmpty()) {
                response.put("message", "No se encontraron movimientos para los datos proporcionados.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // Calcular saldo inicial
            double saldoInicial = 0;
            for (Object[] reg : registros) {
                String fechaStr = reg[0].toString().trim().substring(0, 19);
                Date fechaMovimiento = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(fechaStr);

                if (fechaMovimiento.compareTo(fechaDesde) < 0) {
                    double retiro = reg[8].toString().equals("1") ? Double.parseDouble(reg[3].toString().trim()) : 0;
                    double deposito = reg[8].toString().equals("1") ? 0 : Double.parseDouble(reg[4].toString().trim());
                    saldoInicial += deposito - retiro;
                }
            }

            // Construir y filtrar la lista de movimientos
            List<Map<String, Object>> movimientos = new ArrayList<>();
            double saldo = saldoInicial;

            for (Object[] reg : registros) {
                String fechaStr = reg[0].toString().trim().substring(0, 19);
                Date fechaMovimiento = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(fechaStr);

                if (fechaMovimiento.compareTo(fechaDesde) >= 0 && fechaMovimiento.compareTo(fechaHasta) <= 0) {
                    Map<String, Object> movimiento = new HashMap<>();
                    String caja = reg[14].toString().trim();
                    String documento = reg[12].toString().trim() + " - " + String.format("%06d", Integer.parseInt(reg[13].toString().trim()));
                    String composicion = reg[11].toString().trim();
                    double retiro = reg[8].toString().equals("1") ? Double.parseDouble(reg[3].toString().trim()) : 0;
                    double deposito = reg[8].toString().equals("1") ? 0 : Double.parseDouble(reg[4].toString().trim());

                    saldo += deposito - retiro;

                    movimiento.put("FECHA", fechaStr);
                    movimiento.put("CAJA", caja);
                    movimiento.put("DOCUMENTO", documento);
                    movimiento.put("COMPOSICION", composicion);
                    movimiento.put("RETIRO", retiro);
                    movimiento.put("DEPOSITO", deposito);
                    movimiento.put("SALDO", formatMoneda(saldo));
                    movimientos.add(movimiento);
                }

            }

            if (movimientos.isEmpty()) {
                response.put("message", "No se encontraron movimientos en el rango de fechas especificado.");
                response.put("status", "ERROR004");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            response.put("saldoInicial", formatMoneda(saldoInicial));
            response.put("movimientos", movimientos);
            response.put("status", "INFOUSEROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String formatMoneda(double monto) {
        return String.format("%.2f", monto);
    }

}

