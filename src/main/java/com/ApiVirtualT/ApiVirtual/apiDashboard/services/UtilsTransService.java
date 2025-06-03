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
import libs.PassSecure;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public ResponseEntity<Map<String, Object>> genQRCode(HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            String sql = """
                    SELECT clien_ide_clien, clien_nom_clien, clien_ape_clien, cliac_usu_virtu FROM cnxclien, cnxcliac\s
                    WHERE clien_cod_clien  = :clien_cod_clien
                    AND clien_ide_clien = cliac_ide_clien
                    """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("clien_cod_clien", numSocio);
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontro informacion necesaria para generar el token.");
                response.put("status", "ERROR0033");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            List<Map<String, Object>> cuentasList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> cuenta = new HashMap<>();
                String cedula =   row[0].toString().trim();
                String nombres =  row[1].toString().trim();
                String apellidos  =  row[2].toString().trim();
                String usuarios = row[3].toString().trim();
                String concatenado = nombres + " " + apellidos;
                // Generamos el serial dinámicamente
                String hexfinal = generateHexa();

                // Construimos el string JSON dinámicamente con el serial incluido
                String prueba = String.format(
                        "{\\\"Serial\\\":\\\"%s\\\",\\\"Cedula\\\":\\\"%s\\\",\\\"UserName\\\":\\\"%s\\\",\\\"FullUserName\\\":\\\"%s\\\"}",
                        hexfinal, cedula, usuarios, concatenado
                );
                PassSecure passSecure = new PassSecure();
                String encryptedPrueba = passSecure.encryptPassword(prueba);
                String cleanToken = encryptedPrueba.replaceAll("^\"|\"$", "");
                cuenta.put("token", cleanToken);
                cuentasList.add(cuenta);
            }
            response.put("qr", cuentasList);
            response.put("status", "TOKENQROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR001");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> descrypt(HttpServletRequest token, InterbancariasDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            String sql = """
                    SELECT clien_ide_clien, clien_nom_clien, clien_ape_clien, cliac_usu_virtu FROM cnxclien, cnxcliac\s
                    WHERE clien_cod_clien  = :clien_cod_clien
                    AND clien_ide_clien = cliac_ide_clien
                    """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("clien_cod_clien", numSocio);
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontro informacion necesaria para generar el token.");
                response.put("status", "ERROR0033");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            String cta_banc = dto.getNumTarjeta();
            PassSecure decrypt  = new PassSecure();
            String cta_banco = decrypt.decryptPassword(cta_banc);
            cta_banco = cta_banco.replace("\"", "");
            System.err.println(cta_banco);

            response.put("descrypt", cta_banco);
            response.put("status", "TOKENQROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR001");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> encrypt(HttpServletRequest token, InterbancariasDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            String sql = """
                    SELECT clien_ide_clien, clien_nom_clien, clien_ape_clien, cliac_usu_virtu FROM cnxclien, cnxcliac\s
                    WHERE clien_cod_clien  = :clien_cod_clien
                    AND clien_ide_clien = cliac_ide_clien
                    """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("clien_cod_clien", numSocio);
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontro informacion necesaria para generar el token.");
                response.put("status", "ERROR0033");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            String cta_banc = dto.getNumTarjeta();
            PassSecure encrypt  = new PassSecure();
            String cta_banco = encrypt.encryptPassword(cta_banc);
            String cleaned_tj = cta_banco.replaceAll("^\"|\"$", "").replaceAll("\\\\", "");
            System.err.println(cleaned_tj);

            response.put("encrypt", cleaned_tj);
            response.put("status", "TOKENQROK");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR001");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




    //validar tarjetas
    public ResponseEntity<Map<String, Object>> valTarjetas(HttpServletRequest token, InterbancariasDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");

            String sql = """
                    SELECT clien_ide_clien, clien_nom_clien, clien_ape_clien, cliac_usu_virtu FROM cnxclien, cnxcliac
                    WHERE clien_cod_clien  = :clien_cod_clien
                    AND clien_ide_clien = cliac_ide_clien
                    """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("clien_cod_clien", numSocio);
            List<Object[]> resultados = query.getResultList();
            if (resultados.isEmpty()) {
                response.put("message", "No se encontro informacion necesaria para generar el token.");
                response.put("status", "ERROR0033");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            String cta_banc = dto.getNumTarjeta();
            PassSecure decrypt  = new PassSecure();
            String cta_banco = decrypt.decryptPassword(cta_banc);
            cta_banco = cta_banco.replace("\"", "");
            System.err.println(cta_banco);

            if (cta_banco == null || !cta_banco.matches("\\d+")) {
                response.put("message", "Número de tarjeta inválido");
                response.put("status", "ERROR003");
                response.put("errors", "El numero de tarjeta no puede estar en blanco o contener caracteres diferentes a numeros");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String tipoTarjeta = obtenerTipoTarjeta(cta_banco);
            response.put("tipoTarjeta", tipoTarjeta);

            // Detectar banco
            String banco = detectarBancoEcuador(cta_banco);
            response.put("banco", banco);



            boolean esValida = validarTarjetaLuhn(cta_banco);
            response.put("esValida", esValida);

            if (esValida) {
                response.put("status", "TARJETA_VALIDA");
            } else {
                response.put("status", "TARJETA_INVALIDA");
            }

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR001");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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

            String sqlBeneficiarios = "CALL andprc_beneficiarios_transferencias_totales(:numSocio);";
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

            String sql = "CALL andprc_beneficiariosdirectas(:numSocio);";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("numSocio",numSocio);
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
                    CALL andprc_val_existencia_cta_interna(:numSocio,:cta_banco );
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
                        CALL andprc_estados_cta_interna(:numSocio, :cta_banco, 0);
                    """;
            Query queryUpdate = entityManager.createNativeQuery(sql1);
            queryUpdate.setParameter("numSocio", numSocio);
            queryUpdate.setParameter("cta_banco", cta_banco);
            int rowsUpdated = (int) queryUpdate.getSingleResult();
            System.err.println(rowsUpdated);
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
            String sqlCheck = "CALL andprc_val_existencia_cta_interna (:clienCodClien, :numeroCuenta)";

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
            String sqlUpdate = "CALL andprc_estados_cta_interna(:clienCodClien,:numeroCuenta, 1 )";

            Query queryUpdate = entityManager.createNativeQuery(sqlUpdate);
            queryUpdate.setParameter("clienCodClien", numSocio);
            queryUpdate.setParameter("numeroCuenta", numeroCuenta);
            Integer numresul = (int) queryUpdate.getSingleResult();

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

            String sqlCheck = "CALL andprc_estado_cta_interna(:idPersona, :ctaBanco, 1 ) ";
            Query queryCheck = entityManager.createNativeQuery(sqlCheck);
            queryCheck.setParameter("idPersona", numSocio);
            queryCheck.setParameter("ctaBanco", numeroCuenta);
            List<Object> resultados = queryCheck.getResultList();

            if (!resultados.isEmpty()) {
                // Actualizar beneficiario existente
                String sqlUpdate = " CALL andprc_update_cta_virtual(:idPersona, :ctaBanco, 1, :email, '' ); ";
                Query queryUpdate = entityManager.createNativeQuery(sqlUpdate);
                queryUpdate.setParameter("email", benefiCorreo);
                queryUpdate.setParameter("idPersona", numSocio);
                queryUpdate.setParameter("ctaBanco", numeroCuenta);
                Integer resultado1 = (int) queryUpdate.getSingleResult();
                if(resultado1 >0 ){
                    response.put("message", "Beneficiario actualizado exitosamente.");
                    response.put("status", "GBOK001");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }else{
                    response.put("message", "No se pudo actualizar el beneficiario.");
                    response.put("status", "ERROR003");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

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
                    String sqlInsert = "CALL andprc_inserts_cta_virtual(:idPersona, :ctaBanco, 1, :idBanco, '' , :titular, :descripcion, :fechaAlta, :userName, :cedula, :tipoIdentificacion, :email, :telefonoMovil);";

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
                    Integer resultadon = (int) queryInsert.getSingleResult();
                    if(resultadon > 0){
                        response.put("message", "Beneficiario directo registrado exitosamente.");
                        response.put("status", "GBOK002");
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }else{
                        response.put("message", "Error al guardar Beneficiario directo.");
                        response.put("status", "GBOK202");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }
                }

            }
            response.put("message", "Error al guardar Beneficiario directo.");
            response.put("status", "GBOK458");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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

    public ResponseEntity<Map<String, Object>> consultadetalledepos(VerMovimientoCta dto, HttpServletRequest token) {
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
            String numeroCuenta =  dto.getCtadp_cod_ctadp();

            String sql = """
                    select ctadp_cod_ctadp,ctadp_sal_dispo,
                                ctadp_sal_nodis,ctadp_sal_ndchq,
                                ectad_des_ectad,depos_des_depos 
                           from cnxctadp,cnxectad,cnxdepos 
                           where ctadp_cod_ectad=ectad_cod_ectad and 
                                 ctadp_cod_empre=depos_cod_empre and 
                                 ctadp_cod_ofici=depos_cod_ofici and 
                                 ctadp_cod_depos=depos_cod_depos and 
                                 ctadp_cod_ctadp= :ctadp_cod_ctadp;
        """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("ctadp_cod_ctadp", numeroCuenta);

            @SuppressWarnings("unchecked")
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron cuentas asociadas.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            List<Map<String, Object>> cuentasList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> cuenta = new HashMap<>();
                String ctadp_cod_ctadp = row[0].toString().trim();
                String ctadp_sal_dispo = row[1].toString().trim();
                String ctadp_sal_nodis = row[2].toString().trim();
                String ctadp_sal_ndchq = row[3].toString().trim();
                String ectad_des_ectad = row[4].toString().trim();
                String depos_des_depos = row[5].toString().trim();

                Double salDisForma = Double.parseDouble(ctadp_sal_dispo);
                Double saNoDisForma = Double.parseDouble(ctadp_sal_nodis);
                Double salCheques = Double.parseDouble(ctadp_sal_ndchq);


                String saldoDisFormateado = formatMoneda(salDisForma);
                String saldoNoDisFormateado = formatMoneda(saNoDisForma);
                String SalChequesFormateado = formatMoneda(salCheques);

                cuenta.put("cuenta", ctadp_cod_ctadp);
                cuenta.put("estadoCta", ectad_des_ectad);
                cuenta.put("descripcion", depos_des_depos);
                cuenta.put("saldo_disponible", saldoDisFormateado);
                cuenta.put("salNoDisponibel", saldoNoDisFormateado);
                cuenta.put("salCheques", SalChequesFormateado);
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

    public ResponseEntity<Map<String, Object>> consultadetalleinversiones(HttpServletRequest token) {
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
        select inver_cod_inver, inver_fec_venci, tinve_des_tinve
        from cnxinver, cnxeinve, cnxtinve, cnxclien
        where inver_cod_empre = :cod_empresa
        and inver_cod_clien = :numSocio
        AND clien_ide_clien = :clien_ide_clien
        and inver_cod_einve in (1,2)
        and inver_cod_einve = einve_cod_einve
        and inver_cod_empre = tinve_cod_empre
        and inver_cod_ofici = tinve_cod_ofici
        and inver_cod_tinve = tinve_cod_tinve
        AND inver_cod_clien = clien_cod_clien
        """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("cod_empresa", 69);
            query.setParameter("numSocio", numSocio);
            query.setParameter("clien_ide_clien", clienIdenti);

            @SuppressWarnings("unchecked")
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron inversiones asociadas a esta cuenta.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            List<Map<String, Object>> inversionesList = new ArrayList<>();

            for (Object[] row : resultados) {
                String inver_cod_inver = row[0].toString().trim();
                String inver_fec_venci = row[1].toString().trim();
                String tinve_des_tinve = row[2].toString().trim();

                String sqlDetalle = """
            select 
                inver_cod_inver,
                inver_val_inver,
                inver_fec_venci,
                tinve_des_tinve,
                inver_val_provi,
                (select sum(dminv_val_dminv)
                    from cnxdminv, cnxtmovi
                    where dminv_cod_inver = :inver_cod_inver
                    and dminv_cod_tmovi = tmovi_cod_tmovi
                    and tmovi_cod_tasie = '1') as debe,
                (select sum(dminv_val_dminv)
                    from cnxdminv, cnxtmovi
                    where dminv_cod_inver = :inver_cod_inver
                    and dminv_cod_tmovi = tmovi_cod_tmovi
                    and tmovi_cod_tasie = '2') as haber,
                (select sum(prinv_val_prinv)
                    from cnxprinv, cnxtmovi
                    where prinv_cod_inver = :inver_cod_inver
                    and prinv_cod_tmovi = tmovi_cod_tmovi
                    and tmovi_cod_tasie = '1') as debeinv,
                (select sum(prinv_val_prinv)
                    from cnxprinv, cnxtmovi
                    where prinv_cod_inver = :inver_cod_inver
                    and prinv_cod_tmovi = tmovi_cod_tmovi
                    and tmovi_cod_tasie = '2') as haberinv,
                (select einve_des_einve from cnxeinve
                    where einve_cod_einve = inver_cod_einve) as einve,
                (select tpgin_des_tpgin from cnxtpgin
                    where tpgin_cod_tpgin = inver_cod_tpgin) as des_tpgin
            from cnxinver, cnxeinve, cnxtinve 
            where
                inver_cod_einve = einve_cod_einve and
                inver_cod_empre = tinve_cod_empre and
                inver_cod_ofici = tinve_cod_ofici and
                inver_cod_tinve = tinve_cod_tinve and
                inver_cod_inver = :inver_cod_inver
            """;

                Query queryDetalle = entityManager.createNativeQuery(sqlDetalle);
                queryDetalle.setParameter("inver_cod_inver", inver_cod_inver);

                @SuppressWarnings("unchecked")
                List<Object[]> detalleResultados = queryDetalle.getResultList();

                Map<String, Object> inversion = new HashMap<>();

                if (!detalleResultados.isEmpty()) {
                    Object[] detalle = detalleResultados.get(0);

                    // Parsear los valores numéricos
                    double valorInversion = detalle[1] != null ? Double.parseDouble(detalle[1].toString()) : 0.0;
                    double valorProvision = detalle[4] != null ? Double.parseDouble(detalle[4].toString()) : 0.0;
                    double debe = detalle[5] != null ? Double.parseDouble(detalle[5].toString()) : 0.0;
                    double haber = detalle[6] != null ? Double.parseDouble(detalle[6].toString()) : 0.0;
                    double debeinv = detalle[7] != null ? Double.parseDouble(detalle[7].toString()) : 0.0;
                    double haberinv = detalle[8] != null ? Double.parseDouble(detalle[8].toString()) : 0.0;

                    // Realizar los cálculos
                    double saldo = haber + debe;
                    double saldoPrinv = haberinv - debeinv;
                    double intinv = saldoPrinv + valorProvision;

                    inversion.put("codinversion", detalle[0] != null ? detalle[0].toString().trim() : "");
                    inversion.put("valorinversion", formatMoneda(valorInversion));
                    inversion.put("fechainversion", detalle[2] != null ? detalle[2].toString().trim() : "");
                    inversion.put("descinversion", detalle[3] != null ? detalle[3].toString().trim() : "");
                    inversion.put("valorprovision", formatMoneda(valorProvision));
                    inversion.put("debe", formatMoneda(debe));
                    inversion.put("haber", formatMoneda(haber));
                    inversion.put("debeinv", formatMoneda(debeinv));
                    inversion.put("haberinv", formatMoneda(haberinv));
                    inversion.put("einve", detalle[9] != null ? detalle[9].toString().trim() : "");
                    inversion.put("des_tpgin", detalle[10] != null ? detalle[10].toString().trim() : "");
                    inversion.put("saldo", formatMoneda(saldo));
                    inversion.put("saldo_prinv", formatMoneda(saldoPrinv));
                    inversion.put("interes", formatMoneda(intinv));

                } else {
                    inversion.put("codinversion", inver_cod_inver);
                    inversion.put("fechainversion", inver_fec_venci);
                    inversion.put("descinversion", tinve_des_tinve);
                    inversion.put("valorinversion", "0.00");
                    inversion.put("valorprovision", "0.00");
                    inversion.put("debe", "0.00");
                    inversion.put("haber", "0.00");
                    inversion.put("debeinv", "0.00");
                    inversion.put("haberinv", "0.00");
                    inversion.put("einve", "");
                    inversion.put("des_tpgin", "");
                    inversion.put("saldo", "0.00");
                    inversion.put("saldo_prinv", "0.00");
                    inversion.put("interes", "0.00");
                }
                inversionesList.add(inversion);
            }
            response.put("inversiones", inversionesList);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor.");
            response.put("status", "ERROR003");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> consultadetallecreditos(HttpServletRequest token) {
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

            // Consulta para obtener la lista básica de créditos del cliente
            String sqlCreditos = """
            SELECT credi_cod_credi, credi_fec_venci, tcred_des_tcred, credi_val_credi
            FROM cnxcredi, cnxecred, cnxtcred
            WHERE credi_cod_clien = :numSocio
            AND credi_cod_ecred <> 5
            AND credi_cod_ecred = ecred_cod_ecred
            AND credi_cod_tcred = tcred_cod_tcred
            """;

            Query queryCreditos = entityManager.createNativeQuery(sqlCreditos);
            queryCreditos.setParameter("numSocio", numSocio);

            @SuppressWarnings("unchecked")
            List<Object[]> resultadosCreditos = queryCreditos.getResultList();

            if (resultadosCreditos.isEmpty()) {
                response.put("message", "No se encontraron créditos asociados a este cliente.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            List<Map<String, Object>> creditosList = new ArrayList<>();

            for (Object[] row : resultadosCreditos) {
                String crediCodCredi = row[0].toString().trim();
                String crediFecVenci = row[1].toString().trim();
                String tcredDesTcred = row[2].toString().trim();
                String crediValCredi = row[3].toString().trim();

                // Consulta detallada del crédito
                String sqlDetalleCredito = """
                SELECT 
                    credi_cod_credi as codcred,
                    credi_val_credi as monto,
                    credi_tas_credi as tasa,
                    credi_fec_inici as fec_inici,
                    credi_cod_ofici as ofici,
                    credi_fec_venci as fec_venci,
                    credi_cod_clien as cliente,
                    credi_cod_solic as solicitud,
                    (SELECT tcred_des_tcred FROM cnxtcred
                     WHERE tcred_cod_tcred = credi_cod_tcred) as des_tcred,
                    (SELECT prdrc_des_prdrc FROM cnxprdrc
                     WHERE prdrc_cod_prdrc = credi_cod_prdrc) as recupera,
                    (SELECT SUM(rbdiv_sld_rbdiv) FROM cnxrbdiv
                     WHERE rbdiv_cod_empre = credi_cod_empre 
                     AND rbdiv_cod_ofici = credi_cod_ofici 
                     AND rbdiv_cod_credi = credi_cod_credi 
                     AND rbdiv_cod_rubro = '1') as suma_saldo,
                    (SELECT SUM(rbdiv_sld_rbdiv) FROM cnxrbdiv
                     WHERE rbdiv_cod_empre = credi_cod_empre 
                     AND rbdiv_cod_ofici = credi_cod_ofici 
                     AND rbdiv_cod_credi = credi_cod_credi 
                     AND rbdiv_cod_rubro IN (2,3,5,10)) as interes,
                    (SELECT SUM(rbdiv_sld_rbdiv) FROM cnxrbdiv
                     WHERE rbdiv_cod_empre = credi_cod_empre 
                     AND rbdiv_cod_ofici = credi_cod_ofici 
                     AND rbdiv_cod_credi = credi_cod_credi 
                     AND rbdiv_cod_rubro = '4') as int_mora,
                    (SELECT SUM(rbdiv_sld_rbdiv) FROM cnxrbdiv
                     WHERE rbdiv_cod_empre = credi_cod_empre 
                     AND rbdiv_cod_ofici = credi_cod_ofici 
                     AND rbdiv_cod_credi = credi_cod_credi 
                     AND rbdiv_cod_rubro NOT IN (1,2,3,4,5,10)) as otros,
                    (SELECT SUM(rbdiv_val_rbdiv) FROM cnxdivid, cnxrbdiv
                     WHERE rbdiv_num_divid = divid_num_divid
                     AND rbdiv_cod_credi = divid_cod_credi
                     AND rbdiv_cod_credi = credi_cod_credi
                     AND rbdiv_cod_rubro = 1
                     AND divid_cod_edivi IN (1,4)) as porvencer,
                    (SELECT SUM(rbdiv_val_rbdiv) FROM cnxdivid, cnxrbdiv
                     WHERE rbdiv_num_divid = divid_num_divid
                     AND rbdiv_cod_credi = divid_cod_credi
                     AND rbdiv_cod_credi = credi_cod_credi
                     AND rbdiv_cod_rubro = 1
                     AND divid_cod_edivi IN (2,5)) as reclasificado,
                    (SELECT SUM(rbdiv_val_rbdiv) FROM cnxdivid, cnxrbdiv
                     WHERE rbdiv_num_divid = divid_num_divid
                     AND rbdiv_cod_credi = divid_cod_credi
                     AND rbdiv_cod_credi = credi_cod_credi
                     AND rbdiv_cod_rubro = 1
                     AND divid_cod_edivi IN (3,6)) as vencido,
                    TRIM(credi_ape_clien) || ' ' || TRIM(credi_nom_clien) as nombre
                FROM cnxcredi
                WHERE credi_cod_credi = :credi_cod_credi
                """;

                Query queryDetalle = entityManager.createNativeQuery(sqlDetalleCredito);
                queryDetalle.setParameter("credi_cod_credi", crediCodCredi);

                @SuppressWarnings("unchecked")
                List<Object[]> detalleResultados = queryDetalle.getResultList();

                Map<String, Object> credito = new HashMap<>();
                List<Map<String, Object>> garantesList = new ArrayList<>();

                if (!detalleResultados.isEmpty()) {
                    Object[] detalle = detalleResultados.get(0);

                    // Parsear valores numéricos
                    double monto = detalle[1] != null ? Double.parseDouble(detalle[1].toString()) : 0.0;
                    double tasa = detalle[2] != null ? Double.parseDouble(detalle[2].toString()) : 0.0;
                    double sumaSaldo = detalle[10] != null ? Double.parseDouble(detalle[10].toString()) : 0.0;
                    double interes = detalle[11] != null ? Double.parseDouble(detalle[11].toString()) : 0.0;
                    double intMora = detalle[12] != null ? Double.parseDouble(detalle[12].toString()) : 0.0;
                    double otros = detalle[13] != null ? Double.parseDouble(detalle[13].toString()) : 0.0;
                    double porVencer = detalle[14] != null ? Double.parseDouble(detalle[14].toString()) : 0.0;
                    double reclasificado = detalle[15] != null ? Double.parseDouble(detalle[15].toString()) : 0.0;
                    double vencido = detalle[16] != null ? Double.parseDouble(detalle[16].toString()) : 0.0;

                    // Asignación de valores al crédito
                    credito.put("codigo_credito", detalle[0] != null ? detalle[0].toString().trim() : "");
                    credito.put("monto", formatMoneda(monto));
                    credito.put("tasa", tasa);
                    credito.put("fecha_inicio", detalle[3] != null ? detalle[3].toString().trim() : "");
                    credito.put("oficina", detalle[4] != null ? detalle[4].toString().trim() : "");
                    credito.put("fecha_vencimiento", detalle[5] != null ? detalle[5].toString().trim() : "");
                    credito.put("cliente", detalle[6] != null ? detalle[6].toString().trim() : "");
                    credito.put("solicitud", detalle[7] != null ? detalle[7].toString().trim() : "");
                    credito.put("tipo_credito", detalle[8] != null ? detalle[8].toString().trim() : "");
                    credito.put("recupera", detalle[9] != null ? detalle[9].toString().trim() : "");
                    credito.put("saldo", formatMoneda(sumaSaldo));
                    credito.put("interes", formatMoneda(interes));
                    credito.put("interes_mora", formatMoneda(intMora));
                    credito.put("otros", formatMoneda(otros));
                    credito.put("por_vencer", formatMoneda(porVencer));
                    credito.put("reclasificado", formatMoneda(reclasificado));
                    credito.put("vencido", formatMoneda(vencido));
                    credito.put("nombre_cliente", detalle[17] != null ? detalle[17].toString().trim() : "");


                    String solicitud = credito.get("solicitud").toString();
                    if (!solicitud.isEmpty()) {
                        String sqlGarantes = """
                        SELECT 
                            garan_ape_clien as apellido,
                            garan_nom_clien as nombre,
                            garan_cod_clien as cliente,
                            garan_ide_clien as identidad,
                            garan_dir_domic as dirdomic,
                            garan_tlf_domic as tlfdomic,
                            garan_dir_traba as dirtraba,
                            garan_tlf_traba as tlftraba
                        FROM cnxgaran, cnxgrslt
                        WHERE garan_cod_garan = grslt_cod_garan
                        AND grslt_cod_solic = :solicitud
                        """;

                        Query queryGarantes = entityManager.createNativeQuery(sqlGarantes);
                        queryGarantes.setParameter("solicitud", solicitud);

                        @SuppressWarnings("unchecked")
                        List<Object[]> resultadosGarantes = queryGarantes.getResultList();

                        for (Object[] garante : resultadosGarantes) {
                            Map<String, Object> garanteInfo = new HashMap<>();
                            garanteInfo.put("apellido", garante[0] != null ? garante[0].toString().trim() : "");
                            garanteInfo.put("nombre", garante[1] != null ? garante[1].toString().trim() : "");
                            garanteInfo.put("cliente", garante[2] != null ? garante[2].toString().trim() : "");
                            garanteInfo.put("identidad", garante[3] != null ? garante[3].toString().trim() : "");
                            garanteInfo.put("direccion_domicilio", garante[4] != null ? garante[4].toString().trim() : "");
                            garanteInfo.put("telefono_domicilio", garante[5] != null ? garante[5].toString().trim() : "");
                            garanteInfo.put("direccion_trabajo", garante[6] != null ? garante[6].toString().trim() : "");
                            garanteInfo.put("telefono_trabajo", garante[7] != null ? garante[7].toString().trim() : "");

                            garantesList.add(garanteInfo);
                        }
                    }
                } else {
                    // Datos básicos si no hay detalle
                    credito.put("codigo_credito", crediCodCredi);
                    credito.put("fecha_vencimiento", crediFecVenci);
                    credito.put("tipo_credito", tcredDesTcred);
                    credito.put("monto", formatMoneda(Double.parseDouble(crediValCredi)));

                    credito.put("tasa", "0.00");
                    credito.put("fecha_inicio", "");
                    credito.put("oficina", "");
                    credito.put("cliente", "");
                    credito.put("solicitud", "");
                    credito.put("recupera", "");
                    credito.put("saldo", "0.00");
                    credito.put("interes", "0.00");
                    credito.put("interes_mora", "0.00");
                    credito.put("otros", "0.00");
                    credito.put("por_vencer", "0.00");
                    credito.put("reclasificado", "0.00");
                    credito.put("vencido", "0.00");
                    credito.put("nombre_cliente", "");
                }

                // Agregar lista de garantes al crédito (puede estar vacía)
                credito.put("garantes", garantesList);
                creditosList.add(credito);
            }

            // Construir respuesta final
            response.put("creditos", creditosList);
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
            String sqlListCtaTransferibles = "SELECT ctadp_cod_depos,ctadp_cod_ctadp,depos_des_depos,ctadp_cod_ectad, " +
                    "clien_nom_clien, clien_ape_clien " +
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
            Libs lib = new Libs(entityManager);
            String fecha = lib.obtenerFecha();

            List<Map<String, Object>> cuentas = new ArrayList<>();
            for(Object [] row : listCta){
                String codDepos = row[0].toString().trim();
                String numCta = row[1].toString().trim();
                String descrCta = row[2].toString().trim();
                String ectaCta = row[3].toString().trim();
                String nombreClien = row[4].toString().trim();
                String apelliClien = row[5].toString().trim();
                String saldoCta = obtenerSaldoDisponible(numCta);
                Double saldoDouble = Double.parseDouble(saldoCta);
                String saldoTransfor = formatMoneda(saldoDouble);
                Map<String, Object> cuenta =  new HashMap<>();
                cuenta.put("codigoCta", codDepos);
                cuenta.put("numeroCta", numCta);
                cuenta.put("descrCta", descrCta);
                cuenta.put("estadCta", ectaCta);
                cuenta.put("saldoCta", "$" + saldoTransfor);
                cuenta.put("nombre",nombreClien);
                cuenta.put("apellido", apelliClien);
                cuenta.put("fecha", fecha);
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
                    "CALL andprc_beneficiarios_transferencias_interbancarias(:numSocio, 2);";
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
                beneficiario.put("tipoproducto", tipoProdBanco);
                beneficiario.put("cuentaBanco", ctaBanco);
                beneficiario.put("entidadfinanciera", entidadFinaCta);
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

    //PAGO TARJETAS DE CREDITO
    public ResponseEntity<Map<String, Object>>ctaDebitarPagTarjetas(HttpServletRequest token){
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> allData = new HashMap<>();
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                List<Map<String, Object>> allDataList =  new ArrayList<>();
                allData.put("message", "Datos del token incompletos");
                allData.put("status", "ERRORPGTJER001");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            String sqlListCtaTransferibles = "SELECT ctadp_cod_depos,ctadp_cod_ctadp,depos_des_depos,ctadp_cod_ectad, " +
                    "clien_nom_clien, clien_ape_clien " +
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
                response.put("status", "ERRORPGTJ002");
                response.put("error", "No se encontraron cuentas para transferir en la bdd");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            Libs libreria = new Libs(entityManager);
            String fecha = libreria.obtenerFecha();
            List<Map<String, Object>> cuentas = new ArrayList<>();
            for(Object [] row : listCta){
                String codDepos = row[0].toString().trim();
                String numCta = row[1].toString().trim();
                String descrCta = row[2].toString().trim();
                String ectaCta = row[3].toString().trim();
                String nombre = row[4].toString().trim();
                String apellido = row[5].toString().trim();
                String saldoCta = obtenerSaldoDisponible(numCta);
                Double saldoDouble = Double.parseDouble(saldoCta);
                String saldoTransfor = formatMoneda(saldoDouble);
                Map<String, Object> cuenta =  new HashMap<>();
                cuenta.put("codigoCta", codDepos);
                cuenta.put("numeroCta", numCta);
                cuenta.put("descrCta", descrCta);
                cuenta.put("estadCta", ectaCta);
                cuenta.put("saldoCta", "$" + saldoTransfor);
                cuenta.put("nombre", nombre);
                cuenta.put("apellido", apellido);
                cuenta.put("fecha", fecha);
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
    public ResponseEntity<Map<String, Object>>listBeneficiariosTarjetas(HttpServletRequest token){
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
                    "CALL andprc_beneficiarios_transferencias_interbancarias(:numSocio, 3)";

            Query queryListBeneExternos = entityManager.createNativeQuery(sqlListBeneficiariosExternos);
            queryListBeneExternos.setParameter("numSocio", numSocio);
            List<Object[]> resulBeneExternos = queryListBeneExternos.getResultList();
            if(resulBeneExternos.isEmpty()){
                response.put("message", "No posee tarjetas registradas asociadas a su cuenta");
                response.put("status", "ERRPGTJ002");
                response.put("error", "No se encontraron tarjetas registradas en la bdd");
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
                if (descripcionCta.contains("-")) {
                    descripcionCta = descripcionCta.split("-")[1].trim();
                }

                Map<String, Object> beneficiario =  new HashMap<>();
                beneficiario.put("titular", titularCta);
                beneficiario.put("descripcion", descripcionCta);
                beneficiario.put("email", emailCta);
                beneficiario.put("telefono", tlfCta);
                beneficiario.put("idPersona",idPersona);
                beneficiario.put("tipoIdentificacion", tipoIdentifi);
                beneficiario.put("numIdentificacion", numidentificaciCta);
                beneficiario.put("tipoprodcuto", tipoProdBanco);

                PassSecure encripTarjeta = new PassSecure();
                 String tarjetaEncrip =  encripTarjeta.encryptPassword(ctaBanco);
                tarjetaEncrip = tarjetaEncrip.replaceAll("^\"|\"$", "").replace("\\", "");

                beneficiario.put("tarjetaCredito", tarjetaEncrip);
                beneficiario.put("entidadfinanciera", entidadFinaCta);
                beneficiario.put("tipoCta", tipoCta);
                beneficiarios.add(beneficiario);
            }
            beneficiarios.sort(Comparator.comparing(b -> b.get("titular").toString()));
            response.put("Tarjetas de credito", beneficiarios);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("message", "Error interno del servidor.");
            response.put("status", "ERROR003");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> guardarTarjetas(HttpServletRequest request, InterbancariasDTO dto) {
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
            String numTarjet = dto.getNumTarjeta();
            PassSecure decrypt  = new PassSecure();
            String numTarjeta = decrypt.decryptPassword(numTarjet);
            numTarjeta = numTarjeta.replace("\"", "");
            System.err.println(numTarjeta);

            String tipoCuenta = dto.getTipoCuenta();
            String tipoTarjeta= dto.getTipoTarjeta();
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
            if (numTarjeta == null ) {
                response.put("message", "El número de tarjeta no puede estar vacio o null.");
                response.put("status", "ERROR006");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (!"1".equals(estadoGuardar) && !"0".equals(estadoGuardar)) {
                response.put("message", "El estado solo puede ser '1' o '0'.");
                response.put("status", "ERROR007");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (benefiCorreo == null || !benefiCorreo.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                response.put("message", "El correo del beneficiario tiene una estructura inválida.");
                response.put("status", "ERROR008");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (!"1".equals(estadoGuardar)) {
                response.put("message", "Avanza sin almacenar informacion del usuario !");
                response.put("status", "ERROR009");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFechaYHora();
            System.out.println(fecha);

            // Verificar si ya existe un beneficiario
            String sqlCheck = "CALL andprc_estado_cta_interna(:clienCodClien, :ctaBanco, 2 );   ";
            Query queryCheck = entityManager.createNativeQuery(sqlCheck);
            queryCheck.setParameter("clienCodClien", numSocio);
            queryCheck.setParameter("ctaBanco", numTarjeta);
            List<Object> resultadosCheck = queryCheck.getResultList();
            if (!resultadosCheck.isEmpty()) {
                String sqlUpdate = "CALL  andprc_update_cta_virtual(:clienCodClien, :ctaBanco, 2, :email, :movil );";
                Query queryUpdate = entityManager.createNativeQuery(sqlUpdate);
                queryUpdate.setParameter("email", benefiCorreo);
                queryUpdate.setParameter("movil", movilInter);
                queryUpdate.setParameter("clienCodClien", numSocio);
                queryUpdate.setParameter("ctaBanco", numTarjeta);
                Integer respuestanum = (int) queryUpdate.getSingleResult();
                if(respuestanum >0){
                    response.put("message", "Tarjeta actualizada exitosamente.");
                    response.put("status", "GBOK001");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }else {
                    response.put("message", "Error al actualizar el beneficiario.");
                    response.put("status", "GBOK001");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }else{
                // Insertar un nuevo beneficiario
                String sqlInsert = "CALL andprc_inserts_cta_virtual(:clienCodClien,:ctaBanco, 4,:insBeneInter,:tipoCuenta, :nomTitular, :benefiDetalle, :fechaAlta, :userName, :ideBeneficiario, :tipIden, :email, :movil  );" ;
                Query queryInsertBeneficiario = entityManager.createNativeQuery(sqlInsert);
                queryInsertBeneficiario.setParameter("tipoCuenta",tipoCuenta );
                queryInsertBeneficiario.setParameter("clienCodClien", numSocio);
                queryInsertBeneficiario.setParameter("insBeneInter", codInstitucion);
                queryInsertBeneficiario.setParameter("ctaBanco", numTarjeta);
                queryInsertBeneficiario.setParameter("nomTitular", nombreTitular);
                queryInsertBeneficiario.setParameter("benefiDetalle", numTarjeta + " - " + tipoTarjeta);
                queryInsertBeneficiario.setParameter("fechaAlta", fecha);
                queryInsertBeneficiario.setParameter("userName", clienIdenti);
                queryInsertBeneficiario.setParameter("ideBeneficiario", numIdentificacion);
                queryInsertBeneficiario.setParameter("tipIden", tipoIdentificacion);
                queryInsertBeneficiario.setParameter("email", benefiCorreo);
                queryInsertBeneficiario.setParameter("movil", movilInter);
                Integer resultint = (int) queryInsertBeneficiario.getSingleResult();
                if(resultint > 0){
                    response.put("message", "Tarjeta registrada exitosamente.");
                    response.put("status", "GBOK002");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }else{
                    response.put("message", "ERROR AL registrar su Tarjeta.");
                    response.put("status", "GBOK002");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("status", "ERROR001");
            errorResponse.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //aqui
    public ResponseEntity<Map<String, Object>> eliminarBenefIntrerb(HttpServletRequest token, InterbancariasDTO dto) {
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
            String cta_banc = dto.getNumTarjeta();

            String sql1 = """
                        CALL andprc_update_cta_virtual(:id_persona, :cta_banco, 5, '', '');
                    """;
            Query queryUpdate = entityManager.createNativeQuery(sql1);
            queryUpdate.setParameter("id_persona", numSocio);
            queryUpdate.setParameter("cta_banco", cta_banc);
            int rowsUpdated = (int) queryUpdate.getSingleResult();
            if (rowsUpdated > 0) {
                response.put("message", "Beneficiario eliminado con exito. Registros modificados: " + rowsUpdated);
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



    public ResponseEntity<Map<String, Object>> eliminarTarjetaPj(HttpServletRequest token, InterbancariasDTO dto) {
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
            String cta_banc = dto.getNumTarjeta();
            PassSecure decrypt  = new PassSecure();
            String cta_banco = decrypt.decryptPassword(cta_banc);
            cta_banco = cta_banco.replace("\"", "");
            System.err.println(cta_banco);


            if (cta_banco == null || !cta_banco.matches("\\d+")) {
                response.put("message", "Número de tarjeta inválido");
                response.put("status", "ERROR003");
                response.put("errors", "El numero de tarjeta no puede estar en blanco o contener caracteres diferentes a numeros");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sql = """
                        CALL andprc_estado_cta_interna(:numSocio, :cta_banco, 3);
                    """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("numSocio", numSocio);
            query.setParameter("cta_banco",cta_banco);
            List<Object[]> resultados = query.getResultList();
            if (resultados.isEmpty()) {
                response.put("message", "No se encontro una tarjeta de credito asociada a su cuenta para poder eliminar.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            String sql1 = """
                        CALL andprc_update_cta_virtual(:id_persona, :cta_banco, 3, '', '');
                    """;
            Query queryUpdate = entityManager.createNativeQuery(sql1);
            queryUpdate.setParameter("id_persona", numSocio);
            queryUpdate.setParameter("cta_banco", cta_banco);
            int rowsUpdated = (int) queryUpdate.getSingleResult();
            if (rowsUpdated > 0) {
                response.put("message", "Tarjeta eliminada con exito. Registros modificados: " + rowsUpdated);
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
            if (movilInter != null && !movilInter.isEmpty() && !movilInter.matches("\\d+")) {
                response.put("message", "El número de celular solo puede contener números.");
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
                response.put("status", "ERROR006");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (!"1".equals(estadoGuardar) && !"0".equals(estadoGuardar)) {
                response.put("message", "El estado solo puede ser '1' o '0'.");
                response.put("status", "ERROR007");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (benefiCorreo != null && !benefiCorreo.isEmpty() && !benefiCorreo.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                response.put("message", "El correo del beneficiario tiene una estructura inválida.");
                response.put("status", "ERROR008");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (!"1".equals(estadoGuardar)) {
                response.put("message", "Avanza sin almacenar informacion del usuario !");
                response.put("status", "ERROR009");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFechaYHora();
            System.out.println(fecha);

            // Verificar si ya existe un beneficiario
            String sqlCheck = "CALL andprc_estado_cta_interna(:clienCodClien, :ctaBanco, 4 )";
            Query queryCheck = entityManager.createNativeQuery(sqlCheck);
            queryCheck.setParameter("clienCodClien", numSocio);
            queryCheck.setParameter("ctaBanco", numCuenta);
            //List<Object> resultadosCheck = queryCheck.getResultList();
//            if (!resultadosCheck.isEmpty()) {
//                response.put("message", "Ya existe un beneficiario con los mismos datos.");
//                response.put("status", "ERROR0015");
//                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//            }
            // Si no existe, verificar si hay uno con estado 'F'
//            String sqlCheckInactive = "SELECT * FROM personas_transferencias WHERE id_persona = :clienCodClien AND cta_banco = :ctaBanco AND tipo_trf = 'E' AND vigente = 'F'";
//            Query queryCheckInactive = entityManager.createNativeQuery(sqlCheckInactive);
//            queryCheckInactive.setParameter("clienCodClien", numSocio);
//            queryCheckInactive.setParameter("ctaBanco", numCuenta);
            List<Object> resultadosCheck = queryCheck.getResultList();
            if (!resultadosCheck.isEmpty()) {
                String sqlUpdate = "CALL andprc_update_cta_virtual(:clienCodClien, :ctaBanco, 4, :email, :movil );";
                Query queryUpdate = entityManager.createNativeQuery(sqlUpdate);
                queryUpdate.setParameter("email", benefiCorreo);
                queryUpdate.setParameter("movil", movilInter);
                queryUpdate.setParameter("clienCodClien", numSocio);
                queryUpdate.setParameter("ctaBanco", numCuenta);
                Integer resultados = (int) queryUpdate.getSingleResult();

                if(resultados > 0){
                    response.put("message", "Beneficiario interbancario actualizado exitosamente.");
                    response.put("status", "GBOK001");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }else{
                    response.put("message", "Error al actualizar beneficiario interbancario.");
                    response.put("status", "GBOK0499");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }else{
                // Insertar un nuevo beneficiario
                String sqlInsert = "CALL andprc_inserts_cta_virtual(:clienCodClien,:ctaBanco, 4,:insBeneInter,:tipoCuenta, :nomTitular, :benefiDetalle, :fechaAlta, :userName, :ideBeneficiario, :tipIden, :email, :movil );";
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
                Integer resultnum = (int) queryInsertBeneficiario.getSingleResult();
                if(resultnum > 0){
                    response.put("message", "Beneficiario interbancario registrado exitosamente.");
                    response.put("status", "GBOK002");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }else{
                    response.put("message", "Beneficiario interbancario registrado exitosamente.");
                    response.put("status", "GBOK155");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
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


    public ResponseEntity<Map<String, Object>> movRecientesDirectas(HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();

        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String codclien = (String) token.getAttribute("numSocio");

            if (clienIdenti == null || clienIdenti.trim().isEmpty()) {
                response.put("message", "El identificador del cliente no está presente en el token.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String sql = """
                CALL movimientosrecientesweb(:codclien, '1');
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("codclien", codclien);

            @SuppressWarnings("unchecked")
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron movimientos recientes.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            List<Map<String, Object>> movimientosList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> movimiento = new HashMap<>();
                movimiento.put("codcliente", row[0].toString().trim());
                movimiento.put("ctaorigen", row[1].toString().trim());
                movimiento.put("ctadestino", row[2].toString().trim());
                movimiento.put("ceduladestino", row[3].toString().trim());
                movimiento.put("ip", row[4].toString().trim());
                movimiento.put("titularctadestino", row[5].toString().trim());
                movimiento.put("valor", Double.parseDouble(row[6].toString().trim()));
                movimiento.put("fecha", row[7].toString().trim());
                movimiento.put("codcajas", row[8].toString().trim());
                movimiento.put("descripcion", row[9].toString().trim());
                movimiento.put("tipproducto", row[10].toString().trim());
                movimiento.put("email", row[11].toString().trim());
                movimiento.put("celular", row[12].toString().trim());
                movimientosList.add(movimiento);
            }

            response.put("movimientos", movimientosList);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor.");
            response.put("status", "ERROR003");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> movRecientesInterbancarias(HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();

        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String codclien = (String) token.getAttribute("numSocio");

            if (clienIdenti == null || clienIdenti.trim().isEmpty()) {
                response.put("message", "El identificador del cliente no está presente en el token.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String sql = """
                CALL movimientosrecientesweb(:codclien, '2');
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("codclien", codclien);

            @SuppressWarnings("unchecked")
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron movimientos interbancarios recientes.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            List<Map<String, Object>> movimientosList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> movimiento = new HashMap<>();
                movimiento.put("codcliente", row[0].toString().trim());
                movimiento.put("ctaorigen", row[1].toString().trim());
                movimiento.put("ctadestino", row[2].toString().trim());
                movimiento.put("ceduladestino", row[3].toString().trim());
                movimiento.put("ip", row[4].toString().trim());
                movimiento.put("titularctadestino", row[5].toString().trim());
                movimiento.put("valor", Double.parseDouble(row[6].toString().trim()));
                movimiento.put("fecha", row[7].toString().trim());
                movimiento.put("codcajas", row[8].toString().trim());
                movimiento.put("descripcion", row[9].toString().trim());
                movimiento.put("tipproducto", row[10].toString().trim());
                movimiento.put("email", row[11].toString().trim());
                movimiento.put("celular", row[12].toString().trim());
                movimientosList.add(movimiento);
            }
            response.put("movimientos", movimientosList);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor.");
            response.put("status", "ERROR003");
            response.put("errors", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> movRecientesTarjetas(HttpServletRequest token) {
        Map<String, Object> response = new HashMap<>();

        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String codclien = (String) token.getAttribute("numSocio");

            if (clienIdenti == null || clienIdenti.trim().isEmpty()) {
                response.put("message", "El identificador del cliente no está presente en el token.");
                response.put("status", "ERROR001");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String sql = """
                CALL movimientosrecientesweb(:codclien, '3');
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("codclien", codclien);

            @SuppressWarnings("unchecked")
            List<Object[]> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                response.put("message", "No se encontraron movimientos recientes de pagos tarjetas de credito. ");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            List<Map<String, Object>> movimientosList = new ArrayList<>();
            for (Object[] row : resultados) {
                Map<String, Object> movimiento = new HashMap<>();
                movimiento.put("codcliente", row[0].toString().trim());
                movimiento.put("ctaorigen", row[1].toString().trim());
                movimiento.put("ctadestino", row[2].toString().trim());
                movimiento.put("ceduladestino", row[3].toString().trim());
                movimiento.put("ip", row[4].toString().trim());
                movimiento.put("titularctadestino", row[5].toString().trim());
                movimiento.put("valor", Double.parseDouble(row[6].toString().trim()));
                movimiento.put("fecha", row[7].toString().trim());
                movimiento.put("codcajas", row[8].toString().trim());
                movimiento.put("descripcion", row[9].toString().trim());
                movimiento.put("tipproducto", row[10].toString().trim());
                movimiento.put("email", row[11].toString().trim());
                movimiento.put("celular", row[12].toString().trim());
                movimientosList.add(movimiento);
            }
            response.put("movimientos", movimientosList);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error interno del servidor.");
            response.put("status", "ERROR003");
            response.put("errors", e.getMessage());
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
    private String generateHexa() throws NoSuchAlgorithmException {
        StringBuilder hexaFinal = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String hexa = generateHexSegment();
            hexaFinal.append(hexa);
            if (i < 3) {
                hexaFinal.append("-");
            }
        }
        return hexaFinal.toString();
    }

    private String generateHexSegment() throws NoSuchAlgorithmException {
        Random random = new Random();
        String randomString = String.valueOf(random.nextInt());
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(randomString.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hashBytes.length && hexString.length() < 9; i++) {
            String hex = Integer.toHexString(0xff & hashBytes[i]).toUpperCase();
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.substring(0, 9);
    }
    private String obtenerTipoTarjeta(String numeroTarjeta) {
        if (numeroTarjeta.startsWith("4")) {
            return "Visa";
        } else if (numeroTarjeta.startsWith("5")) {
            return "MasterCard";
        } else if (numeroTarjeta.startsWith("34") || numeroTarjeta.startsWith("37")) {
            return "American Express";
        } else if (numeroTarjeta.startsWith("6")) {
            return "Discover";
        } else if (numeroTarjeta.startsWith("35")) {
            return "JCB";
        } else if (numeroTarjeta.startsWith("30") || numeroTarjeta.startsWith("36") || numeroTarjeta.startsWith("38") || numeroTarjeta.startsWith("39")) {
            return "Diners Club";
        } else {
            return "Desconocido";
        }
    }
    private static String detectarBancoEcuador(String numeroTarjeta) {
        // Diccionario actualizado de BINs para bancos ecuatorianos (2025)
        Map<String, String[]> binsBancos = new HashMap<>();
        binsBancos.put("Pichincha", new String[]{"403750", "409851", "416431", "416432", "479931",
                "521848", "527593", "528851", "530722", "603488"});
        binsBancos.put("Guayaquil", new String[]{"403751", "409852", "416433", "479932", "485942",
                "522846", "528852", "530723", "557700", "603489"});
        binsBancos.put("Produbanco", new String[]{"403752", "409853", "416434", "479933", "485943",
                "512269", "522847", "528853", "530724", "557701"});
        binsBancos.put("Pacifico", new String[]{"403753", "409854", "416435", "479934", "485944",
                "512270", "522848", "528854", "530725", "557702"});
        binsBancos.put("Bolivariano", new String[]{"403754", "409855", "416436"});
        binsBancos.put("Internacional", new String[]{"403755", "409856", "416437"});
        binsBancos.put("Ambato", new String[]{"403756", "409857", "416438"});

        // Verificar cada banco
        for (Map.Entry<String, String[]> entry : binsBancos.entrySet()) {
            String banco = entry.getKey();
            String[] bins = entry.getValue();

            for (String bin : bins) {
                if (numeroTarjeta.startsWith(bin)) {
                    return banco;
                }
            }
        }

        // Si no se encuentra en los principales, verificar rangos genéricos
        if (numeroTarjeta.length() > 0) {
            char primerDigito = numeroTarjeta.charAt(0);
            switch (primerDigito) {
                case '4': return "Visa (Banco no identificado)";
                case '5': return "Mastercard (Banco no identificado)";
                case '3': return "American Express/Diners (Banco no identificado)";
                case '6': return "Discover/UnionPay (Banco no identificado)";
                default: return "Banco no identificado";
            }
        }

        return "Banco no identificado";
    }


    private boolean validarTarjetaLuhn(String numeroTarjeta) {
        int suma = 0;
        boolean alternar = false;

        for (int i = numeroTarjeta.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(numeroTarjeta.charAt(i));

            if (alternar) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            suma += n;
            alternar = !alternar;
        }
        return (suma % 10 == 0);
    }

}

