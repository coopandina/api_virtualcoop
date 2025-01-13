package com.ApiVirtualT.ApiVirtual.apiDashboard.services;
import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.TransDirecDTO;
import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.verMovimientoCta;
import envioCorreo.sendEmail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sms.SendSMS;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Transactional
@Service
@RequiredArgsConstructor
public class Dashservice {
    @PersistenceContext
    private EntityManager entityManager;

    int intentosRealizadoTokenFallos = 0;

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

    public ResponseEntity<Map<String, Object>> buscarPorNombreApellido(HttpServletRequest token, verMovimientoCta dto ) {
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
            // Convertir la entrada a mayúsculas
            entradaBusqueda = entradaBusqueda.toUpperCase();

            // Validación para asegurarse de que la entrada solo contiene texto (letras y espacios)
            if (!entradaBusqueda.matches("[A-Za-zÁÉÍÓÚáéíóú\\s]+")) {
                response.put("message", "La entrada de búsqueda solo debe contener texto válido (sin números ni caracteres especiales).");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sqlBeneficiarios =
                    "SELECT p.titular, p.descripcion, p.email, p.telefono_movil, p.id_persona, p.cta_banco, " +
                            "       CASE WHEN p.tipo_trf = 'E' THEN (SELECT ifspi_nom_ifspi FROM cnxifspi WHERE ifspi_cod_ifspi = p.id_banco) ELSE NULL END AS entidad_financiera, p.tipo_trf " +
                            "FROM personas_transferencias p " +
                            "WHERE p.id_persona = :numSocio " +
                            "AND p.vigente = 'T' " +
                            "AND (p.tipo_trf = 'I' OR (p.tipo_trf = 'E' AND p.tipo_prod_banc IN ('AH', 'CC'))) " +
                            "AND p.titular LIKE :titular";
            Query queryBuscarBeneficiarios = entityManager.createNativeQuery(sqlBeneficiarios);
            queryBuscarBeneficiarios.setParameter("numSocio", numSocio);
            queryBuscarBeneficiarios.setParameter("titular", "%" + entradaBusqueda + "%");
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
    public ResponseEntity<Map<String, Object>> validarBeneficiario(HttpServletRequest token, verMovimientoCta dto) {
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
    public ResponseEntity<Map<String, Object>> guardarBenefiDirecto(HttpServletRequest token, verMovimientoCta dto) {
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
            if (benefiCorreo == null || !benefiCorreo.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                response.put("message", "El correo del beneficiario tiene una estructura inválida.");
                response.put("status", "ERROR004");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validar estadoGuardar
            if (!"1".equals(estadoGuardar)) {
                response.put("message", "El estado Guardar no permite realizar esta operación.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String sqlCheck = "SELECT * FROM personas_transferencias WHERE id_persona = :idPersona " +
                    "AND tipo_trf = 'I' AND cta_banco = :ctaBanco AND vigente = 'T'";
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

                String sqlFechaHora = "CALL cnxprc_fecha_hora()";
                Query queryFecha = entityManager.createNativeQuery(sqlFechaHora);
                List<Object[]> resultadoFecha = queryFecha.getResultList();

                if (resultadoFecha.isEmpty()) {
                    throw new Exception("No se pudo obtener la fecha actual del sistema.");
                }

                String fecha = (String) resultadoFecha.get(0)[2];
                fecha = fecha.trim();

                if (fecha == null || fecha.isEmpty()) {
                    throw new IllegalArgumentException("La fecha obtenida no es válida.");
                }
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
            int ctropera = 3;

            String sql = """
            SELECT MIN(ctadp_cod_ctadp) AS cuenta, ctadp_cod_depos, depos_des_depos, ctadp_cod_ectad
            FROM cnxclien, cnxctadp, cnxdepos, cnxopdep
            WHERE clien_ide_clien = :txtideclien
              AND ctadp_cod_empre = clien_cod_empre
              AND ctadp_cod_ofici = clien_cod_ofici
              AND ctadp_cod_clien = clien_cod_clien
              AND ctadp_cod_depos IN (1)
              AND depos_cod_empre = ctadp_cod_empre
              AND depos_cod_ofici = ctadp_cod_ofici
              AND depos_cod_depos = ctadp_cod_depos
              AND depos_ctr_opera = 0
              AND depos_cod_moned = 2
              AND opdep_cod_empre = ctadp_cod_empre
              AND opdep_cod_ofici = ctadp_cod_ofici
              AND opdep_cod_depos = ctadp_cod_depos
              AND opdep_cod_ectad = ctadp_cod_ectad
              AND opdep_cod_toper = :ctropera
            GROUP BY 2, 3, 4
            ORDER BY ctadp_cod_depos
        """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("txtideclien", clienIdenti);
            query.setParameter("ctropera", ctropera);

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
                String txtdesdepos = row[2].toString().trim();
                // Llamamos a las funciones y obtenemos el saldo y el maximo de retiro como String
                String saldoDisponibleStr = obtenerSaldoDisponible(txtcodctadp);
                String maximoRetiroStr = verInfRetMax(txtcodctadp);
                // Convertimos los valores a Double si es necesario para realizar cálculos
                Double saldoDisponible = Double.parseDouble(saldoDisponibleStr);
                Double maximoRetiro = Double.parseDouble(maximoRetiroStr);

                cuenta.put("cuenta", txtcodctadp);
                cuenta.put("descripcion", txtdesdepos);
                cuenta.put("saldo_disponible", formatMoneda(saldoDisponible));
                cuenta.put("maximo_retiro", formatMoneda(maximoRetiro));
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
    public ResponseEntity<Map<String, Object>> srtGrabarDir(HttpServletRequest token, TransDirecDTO dto) {
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            Map<String, Object> response = new HashMap<>();

            // Validación de datos del token
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                response.put("message", "Datos del token incompletos");
                response.put("status", "AA022");
                response.put("error", "ERROR EN LA AUTENTICACIÓN");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            String numeroCuentaEnvio = dto.getCtaEnvio();
            String numeroCtaDestino = dto.getCtaDestino();
            String descripcionTrf = dto.getTxtdettrnsf();
            Float valTransferencia = dto.getValtrans();
            // Validación de cuenta origen
            if (numeroCuentaEnvio == null || !numeroCuentaEnvio.matches("\\d{12}")) {
                response.put("message", "El número de cuenta origen debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validación de cuenta destino
            if (numeroCtaDestino == null || !numeroCtaDestino.matches("\\d{12}")) {
                response.put("message", "El número de cuenta destino debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validación del código temporal
            if (dto.getCodTempTransDirec() == null || !dto.getCodTempTransDirec().matches("\\d{6}")) {
                response.put("message", "Código de seguridad inválido");
                response.put("status", "AA023");
                response.put("error", "El código debe contener exactamente 6 dígitos");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validación de la descripción de la transferencia (debe tener entre 1 y 250 caracteres)
            if (descripcionTrf == null || descripcionTrf.trim().isEmpty() || descripcionTrf.length() > 250) {
                response.put("message", "La descripción de la transferencia no puede estar vacía y debe tener como máximo 250 caracteres.");
                response.put("status", "ERROR004");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validación del monto de la transferencia (debe ser un número decimal con hasta 16 dígitos, 2 de los cuales pueden ser decimales)
            if (valTransferencia == null || valTransferencia <= 0 || !valTransferencia.toString().matches("^\\d{1,14}(\\.\\d{1,2})?$")) {
                response.put("message", "El monto de la transferencia debe ser un número positivo con hasta 14 dígitos enteros y 2 decimales.");
                response.put("status", "ERROR005");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Verificación del token en la base de datos
            String sqlVerificaTokenBDD = "SELECT codaccess_codigo_temporal FROM vircodaccess " +
                    "WHERE codaccess_cedula = :codaccess_cedula AND codaccess_usuario = :codaccess_usuario " +
                    "AND codaccess_estado = :codaccess_estado AND codsms_codigo = '3' ";
            Query queryVerificaTokenBDD = entityManager.createNativeQuery(sqlVerificaTokenBDD);
            queryVerificaTokenBDD.setParameter("codaccess_cedula", clienIdenti);
            queryVerificaTokenBDD.setParameter("codaccess_usuario", cliacUsuVirtu);
            queryVerificaTokenBDD.setParameter("codaccess_estado", "1");

            List<Object[]> resultsTokenBDD = queryVerificaTokenBDD.getResultList();
            if (resultsTokenBDD.isEmpty()) {
                response.put("message", "CODIGO TEMPORAL EXPIRADO, POR EXCEDER LOS 4 MINUTOS");
                response.put("status", "AA027");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String saldoDisponible = obtenerSaldoDisponible(numeroCuentaEnvio);
            System.out.println(saldoDisponible);
            Float saldoDispoParse = Float.parseFloat(saldoDisponible);

            String tokenFromDB = (String) queryVerificaTokenBDD.getSingleResult();
            if (!tokenFromDB.trim().equals(dto.getCodTempTransDirec())) {
                intentosRealizadoTokenFallos++;
                if (intentosRealizadoTokenFallos >= 3) {
                    // Bloquear usuario
                    String sqlBloqUser = "UPDATE cnxcliac SET cliac_ctr_bloq = :bloqueo WHERE cliac_usu_virtu = :username";
                    Query resultBloqUser = entityManager.createNativeQuery(sqlBloqUser);
                    resultBloqUser.setParameter("bloqueo", "0");
                    resultBloqUser.setParameter("username", cliacUsuVirtu);

                    try {
                        int rowsUpdated = resultBloqUser.executeUpdate();
                        if (rowsUpdated > 0) {
                            // Obtener datos para el correo
                            String sqlDatosCorreoIngreso = "SELECT clien_ape_clien, clien_nom_clien, clien_dir_email " +
                                    "FROM cnxclien, cnxcliac WHERE cliac_usu_virtu = :username " +
                                    "AND clien_ide_clien = cliac_ide_clien";
                            Query resulDatosCorreoIngreso = entityManager.createNativeQuery(sqlDatosCorreoIngreso);
                            resulDatosCorreoIngreso.setParameter("username", cliacUsuVirtu);

                            String FechaHora = obtenerHoraActual();
                            List<Object[]> results2 = resulDatosCorreoIngreso.getResultList();

                            for (Object[] row2 : results2) {
                                String clienApellidos = row2[0].toString().trim();
                                String clienNombres = row2[1].toString().trim();
                                String clienEmail = row2[2].toString().trim();
                                String IpIngreso = localIP();
                                sendEmail emailBloq = new sendEmail();
                                emailBloq.sendEmailBloqueo(clienApellidos, clienNombres, FechaHora, clienEmail, IpIngreso);
                            }

                            intentosRealizadoTokenFallos = 0;
                            response.put("message", "Usuario bloqueado por exceder límite de intentos");
                            response.put("status", "AA025");
                            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                        }
                    } catch (Exception e) {
                        response.put("message", "Error al intentar bloquear el usuario");
                        response.put("status", "AA024");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }
                } else {
                    response.put("message", "Token incorrecto. Intentos restantes: " + (3 - intentosRealizadoTokenFallos));
                    response.put("status", "AA023");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
            if (saldoDispoParse >= valTransferencia){
                String sqlQuery = "SELECT clien_cod_empre, clien_cod_ofici, ctadp_cod_ctadp " +
                        "FROM cnxctadp, cnxclien, cnxcliac " +
                        "WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp " +
                        "AND ctadp_cod_depos = :ctadp_cod_depos " +
                        "AND ctadp_cod_ectad = :ctadp_cod_ectad " +
                        "AND ctadp_cod_clien = clien_cod_clien " +
                        "AND clien_ide_clien = cliac_ide_clien";

                // Consulta cuenta origen
                Query query = entityManager.createNativeQuery(sqlQuery);
                query.setParameter("ctadp_cod_ctadp", numeroCuentaEnvio);
                query.setParameter("ctadp_cod_depos", "1");
                query.setParameter("ctadp_cod_ectad", "1");
                List<Object[]> results = query.getResultList();

                // Consulta cuenta destino
                Query query1 = entityManager.createNativeQuery(sqlQuery);
                query1.setParameter("ctadp_cod_ctadp", numeroCtaDestino);
                query1.setParameter("ctadp_cod_depos", "1");
                query1.setParameter("ctadp_cod_ectad", "1");
                List<Object[]> results1 = query1.getResultList();

                // Procesar resultados cuenta origen
                if (results.isEmpty()) {
                    response.put("message", "Cuenta origen no encontrada o inválida");
                    response.put("status", "ERROR004");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                // Procesar resultados cuenta destino
                if (results1.isEmpty()) {
                    response.put("message", "Cuenta destino no encontrada o inválida");
                    response.put("status", "ERROR005");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                // Extraer datos de las cuentas
                Object[] resultEnvio = results.get(0);
                Object[] resultDestino = results1.get(0);

                String clienCodEmpreEnvio = resultEnvio[0].toString().trim();
                String clienCodOficiEnvio = resultEnvio[1].toString().trim();
                String ctadpCodCtadpEnvio = resultEnvio[2].toString().trim();

                String clienCodEmpreDestino = resultDestino[0].toString().trim();
                String clienCodOficiDestino = resultDestino[1].toString().trim();
                String ctadpCodCtadpDestino = resultDestino[2].toString().trim();

                if (clienCodOficiEnvio.equals(clienCodOficiDestino)) {
                    // Transferencia en la misma oficina
                    String callSetLockProcedure = "CALL cnxprc_setea_lockm()";
                    Query lockProcedureQuery = entityManager.createNativeQuery(callSetLockProcedure);
                    lockProcedureQuery.executeUpdate();

                    String callTransferProcedure = "CALL cnxprc_reg_trfwb(:clienCodEmpreEnvio, :clienCodOficiEnvio, '803', " +
                            ":descripcionTrf, :ctadpCodCtadpEnvio, :ctadpCodCtadpDestino, :valTransferencia)";
                    Query queryProcedure = entityManager.createNativeQuery(callTransferProcedure);
                    queryProcedure.setParameter("clienCodEmpreEnvio", clienCodEmpreEnvio);
                    queryProcedure.setParameter("clienCodOficiEnvio", clienCodOficiEnvio);
                    queryProcedure.setParameter("descripcionTrf", descripcionTrf);
                    queryProcedure.setParameter("ctadpCodCtadpEnvio", ctadpCodCtadpEnvio);
                    queryProcedure.setParameter("ctadpCodCtadpDestino", ctadpCodCtadpDestino);
                    queryProcedure.setParameter("valTransferencia", valTransferencia);

                    Object result = queryProcedure.getSingleResult();
                    int returnValue = Integer.parseInt(result.toString());

                    String sqlInfoEnvio = "SELECT ofici_nom_ofici,clien_dir_email,clien_ape_clien,clien_nom_clien, clien_tlf_celul,clien_cod_clien " +
                            "FROM cnxctadp, cnxclien, cnxofici " +
                            "WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp " +
                            "AND ctadp_cod_depos= 1 "+
                            "AND ctadp_cod_ectad= 1 " +
                            "AND clien_cod_ofici = ofici_cod_ofici "+
                            "AND ctadp_cod_clien=clien_cod_clien";
                    Query queryParamsEnvio = entityManager.createNativeQuery(sqlInfoEnvio);
                    queryParamsEnvio.setParameter("ctadp_cod_ctadp", ctadpCodCtadpEnvio);

                    String sqlInfoRecibe = "SELECT ofici_nom_ofici,clien_dir_email,clien_ape_clien,clien_nom_clien, clien_tlf_celul,clien_cod_clien " +
                            "FROM cnxctadp, cnxclien, cnxofici " +
                            "WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp " +
                            "AND ctadp_cod_depos= 1 "+
                            "AND ctadp_cod_ectad= 1" +
                            "AND clien_cod_ofici = ofici_cod_ofici "+
                            "AND ctadp_cod_clien=clien_cod_clien";
                    Query queryParamsRecibe = entityManager.createNativeQuery(sqlInfoRecibe);
                    queryParamsRecibe.setParameter("ctadp_cod_ctadp", ctadpCodCtadpDestino);

                    List<Object[]> resultsInfoCtaEnvio = queryParamsEnvio.getResultList();
                    List<Object[]> resultsInfoCtaRecibe = queryParamsRecibe.getResultList();

                    // Procesar datos de la cuenta de envío
                    if (!resultsInfoCtaEnvio.isEmpty()) {
                        Object[] rowEnvio = resultsInfoCtaEnvio.get(0); // Solo un resultado esperado
                        Map<String, String> infCtaEnvio = new HashMap<>();
                        infCtaEnvio.put("nombreOficina", rowEnvio[0].toString().trim());
                        infCtaEnvio.put("email", rowEnvio[1].toString().trim());
                        infCtaEnvio.put("apellido", rowEnvio[2].toString().trim());
                        infCtaEnvio.put("nombre", rowEnvio[3].toString().trim());
                        infCtaEnvio.put("telefono", rowEnvio[4].toString().trim());
                        infCtaEnvio.put("codigoCliente", rowEnvio[5].toString().trim());

                        response.put("informacionCtaEnvio", infCtaEnvio);
                    }
                    // Procesar datos de la cuenta de recepción
                    if (!resultsInfoCtaRecibe.isEmpty()) {
                        Object[] rowRecibe = resultsInfoCtaRecibe.get(0); // Solo un resultado esperado
                        Map<String, String> infCtaRecibe = new HashMap<>();
                        infCtaRecibe.put("nombreOficina", rowRecibe[0].toString().trim());
                        infCtaRecibe.put("email", rowRecibe[1].toString().trim());
                        infCtaRecibe.put("apellido", rowRecibe[2].toString().trim());
                        infCtaRecibe.put("nombre", rowRecibe[3].toString().trim());
                        infCtaRecibe.put("telefono", rowRecibe[4].toString().trim());
                        infCtaRecibe.put("codigoCliente", rowRecibe[5].toString().trim());

                        response.put("informacionCtaRecibe", infCtaRecibe);
                    }
                        response.put("message", "TRANSFERENCIA REALIZADA CON ÉXITO :)");
                        response.put("numTransferencia", returnValue);
                        response.put("status", "DTROK0005");

                } else {
                    // Transferencia entre diferentes oficinas
                    String callTransferProcedure = "CALL cnxprc_trnsf_rmtwb(:clienCodEmpreEnvio, :clienCodOficiEnvio, '803', " +
                            ":descripcionTrf, :ctadpCodCtadpEnvio, :ctadpCodCtadpDestino, :valTransferencia)";
                    Query queryProcedure = entityManager.createNativeQuery(callTransferProcedure);
                    queryProcedure.setParameter("clienCodEmpreEnvio", clienCodEmpreEnvio);
                    queryProcedure.setParameter("clienCodOficiEnvio", clienCodOficiEnvio);
                    queryProcedure.setParameter("descripcionTrf", descripcionTrf);
                    queryProcedure.setParameter("ctadpCodCtadpEnvio", ctadpCodCtadpEnvio);
                    queryProcedure.setParameter("ctadpCodCtadpDestino", ctadpCodCtadpDestino);
                    queryProcedure.setParameter("valTransferencia", valTransferencia);

                    Object result = queryProcedure.getSingleResult();
                    int returnValue = Integer.parseInt(result.toString());

                    response.put("message", "TRANSFERENCIA REALIZADA CON ÉXITO :) DIFERENTES OFICINAS");
                    response.put("numTransferencia", returnValue);
                    response.put("status", "TRFOK0045");
                }
                return new ResponseEntity<>(response, HttpStatus.OK);

            }
                response.put("message", "MONTO INSUFICIENTE PARA REALIZAR LA TRANSFERENCIA ");
                response.put("error", "ERROR005");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String obtenerSaldoDisponible(String txtcodctadp) throws Exception {
        try {
            // 1. Obtener la fecha actual del sistema llamando a un procedimiento almacenado
            String sqlFechaHora = "CALL cnxprc_fecha_hora()";
            Query queryFecha = entityManager.createNativeQuery(sqlFechaHora);
            List<Object[]> resultadoFecha = queryFecha.getResultList();

            if (resultadoFecha.isEmpty()) {
                throw new Exception("No se pudo obtener la fecha actual del sistema.");
            }
            String fecha = resultadoFecha.get(0)[0].toString().trim();
            System.out.println(fecha);
            // 2. Ejecutar el procedimiento almacenado para obtener el saldo disponible
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

    public ResponseEntity<Map<String, Object>> obtenerMovimientos(verMovimientoCta dto, HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();
        try {
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            if (clienIdenti == null || clienIdenti.trim().isEmpty()) {
                response.put("message", "El identificador del cliente no está presente en el token.");
                response.put("status", "ERROR001");
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
    public static String localIP() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "No disponible";
        }
    }
    public static String obtenerHoraActual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

}

