package com.ApiVirtualT.ApiVirtual.apiDashboard.services;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.TokenExpirationService;
import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.TransferenciasDTO;
import com.ApiVirtualT.ApiVirtual.libs.Libs;
import envioCorreo.sendEmail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sms.SendSMS;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Transactional
@Service
@RequiredArgsConstructor
public class TransferenciasDirecService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TokenExpirationService tokenExpirationService;
    int intentosRealizadoTokenFallos = 0;
    int intentosRealizadoTokenFallosInterban = 0;

    public ResponseEntity<Map<String, Object>> srtGrabarDir(HttpServletRequest token, TransferenciasDTO dto) {
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
            if (numeroCuentaEnvio == null || !numeroCuentaEnvio.matches("\\d{12}")) {
                response.put("message", "El número de cuenta origen debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (numeroCtaDestino == null || !numeroCtaDestino.matches("\\d{12}")) {
                response.put("message", "El número de cuenta destino debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (dto.getCodTempTransDirec() == null || !dto.getCodTempTransDirec().matches("\\d{6}")) {
                response.put("message", "Código de seguridad inválido");
                response.put("status", "AA023");
                response.put("error", "El código debe contener exactamente 6 dígitos");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (descripcionTrf == null || descripcionTrf.trim().isEmpty() || descripcionTrf.length() > 250) {
                response.put("message", "La descripción de la transferencia no puede estar vacía y debe tener como máximo 250 caracteres.");
                response.put("status", "ERROR004");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (valTransferencia == null || valTransferencia <= 0 || !valTransferencia.toString().matches("^\\d{1,14}(\\.\\d{1,2})?$")) {
                response.put("message", "El monto de la transferencia debe ser un número positivo con hasta 14 dígitos enteros y 2 decimales.");
                response.put("status", "ERROR005");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
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
                            Libs fechaHoraService = new Libs(entityManager);
                            String FechaHora = fechaHoraService.obtenerFechaYHora();

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
                        "AND ctadp_cod_depos IN (1,2) " +
                        "AND ctadp_cod_ectad = :ctadp_cod_ectad " +
                        "AND ctadp_cod_clien = clien_cod_clien " +
                        "AND clien_ide_clien = cliac_ide_clien";
                // Consulta cuenta origen
                Query query = entityManager.createNativeQuery(sqlQuery);
                query.setParameter("ctadp_cod_ctadp", numeroCuentaEnvio);
                query.setParameter("ctadp_cod_ectad", "1");
                List<Object[]> results = query.getResultList();
                // Consulta cuenta destino
                Query query1 = entityManager.createNativeQuery(sqlQuery);
                query1.setParameter("ctadp_cod_ctadp", numeroCtaDestino);
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
                            "AND ctadp_cod_depos IN (1,2) "+
                            "AND ctadp_cod_ectad= 1 " +
                            "AND clien_cod_ofici = ofici_cod_ofici "+
                            "AND ctadp_cod_clien=clien_cod_clien";
                    Query queryParamsEnvio = entityManager.createNativeQuery(sqlInfoEnvio);
                    queryParamsEnvio.setParameter("ctadp_cod_ctadp", ctadpCodCtadpEnvio);

                    String sqlInfoRecibe = "SELECT ofici_nom_ofici,clien_dir_email,clien_ape_clien,clien_nom_clien, clien_tlf_celul,clien_cod_clien " +
                            "FROM cnxctadp, cnxclien, cnxofici " +
                            "WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp " +
                            "AND ctadp_cod_depos IN (1,2) "+
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
    public ResponseEntity<Map<String, Object>> srtGrabarInterban(HttpServletRequest token, TransferenciasDTO dto) {
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
            if (numeroCuentaEnvio == null || !numeroCuentaEnvio.matches("\\d{12}")) {
                response.put("message", "El número de cuenta origen debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (numeroCtaDestino == null || !numeroCtaDestino.matches("\\d+")) {
                response.put("message", "El número de cuenta destino debe contener únicamente dígitos numéricos.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (dto.getCodTempTransDirec() == null || !dto.getCodTempTransDirec().matches("\\d{6}")) {
                response.put("message", "Código de seguridad inválido");
                response.put("status", "AA023");
                response.put("error", "El código debe contener exactamente 6 dígitos");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (descripcionTrf == null || descripcionTrf.trim().isEmpty() || descripcionTrf.length() > 250) {
                response.put("message", "La descripción de la transferencia no puede estar vacía y debe tener como máximo 250 caracteres.");
                response.put("status", "ERROR004");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (valTransferencia == null || valTransferencia <= 0 || !valTransferencia.toString().matches("^\\d{1,14}(\\.\\d{1,2})?$")) {
                response.put("message", "El monto de la transferencia debe ser un número positivo con hasta 14 dígitos enteros y 2 decimales.");
                response.put("status", "ERROR005");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sqlVerificaTokenBDD = "SELECT codaccess_codigo_temporal FROM vircodaccess " +
                    "WHERE codaccess_cedula = :codaccess_cedula AND codaccess_usuario = :codaccess_usuario " +
                    "AND codaccess_estado = :codaccess_estado AND codsms_codigo = '2' ";
            Query queryVerificaTokenBDD = entityManager.createNativeQuery(sqlVerificaTokenBDD);
            queryVerificaTokenBDD.setParameter("codaccess_cedula", clienIdenti);
            queryVerificaTokenBDD.setParameter("codaccess_usuario", cliacUsuVirtu);
            queryVerificaTokenBDD.setParameter("codaccess_estado", "1");

            List<Object[]> resultsTokenBDD = queryVerificaTokenBDD.getResultList();
            if (resultsTokenBDD.isEmpty()) {
                response.put("message", "CODIGO TEMPORAL EXPIRADO, POR EXCEDER LOS 4 MINUTOS");
                response.put("status", "AA041");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String saldoDisponible = obtenerSaldoDisponible(numeroCuentaEnvio);
            System.out.println(saldoDisponible);
            Float saldoDispoParse = Float.parseFloat(saldoDisponible);

            String tokenFromDB = (String) queryVerificaTokenBDD.getSingleResult();
            if (!tokenFromDB.trim().equals(dto.getCodTempTransDirec())) {
                intentosRealizadoTokenFallosInterban++;
                if (intentosRealizadoTokenFallosInterban >= 3) {
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
                            Libs fechaHoraService = new Libs(entityManager);
                            String FechaHora = fechaHoraService.obtenerFechaYHora();

                            List<Object[]> results2 = resulDatosCorreoIngreso.getResultList();

                            for (Object[] row2 : results2) {
                                String clienApellidos = row2[0].toString().trim();
                                String clienNombres = row2[1].toString().trim();
                                String clienEmail = row2[2].toString().trim();
                                String IpIngreso = localIP();
                                sendEmail emailBloq = new sendEmail();
                                emailBloq.sendEmailBloqueo(clienApellidos, clienNombres, FechaHora, clienEmail, IpIngreso);
                            }

                            intentosRealizadoTokenFallosInterban = 0;
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
                    response.put("message", "Token incorrecto. Intentos restantes: " + (3 - intentosRealizadoTokenFallosInterban));
                    response.put("status", "AA023");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
            if (saldoDispoParse >= valTransferencia){
                String sqlQuery = """
                SELECT ctadp_cod_empre, ctadp_cod_ofici, clien_ape_clien, clien_nom_clien, ctadp_cod_clien, clien_ide_clien 
                FROM cnxctadp, cnxclien 
                WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp AND ctadp_cod_clien = :ctadp_cod_clien 
                AND ctadp_cod_ectad = '1' 
                AND ctadp_cod_clien = clien_cod_clien
                        """;
                String sqlQuery1 = """
                SELECT id_banco, cta_banco, titular, cedula
                FROM personas_transferencias  WHERE id_persona = :id_persona
                AND cta_banco = :cta_banco
                AND tipo_trf = 'E' AND vigente = 'T'
                """;
                // Consulta cuenta origen
                Query query = entityManager.createNativeQuery(sqlQuery);
                query.setParameter("ctadp_cod_ctadp", numeroCuentaEnvio);
                query.setParameter("ctadp_cod_clien", numSocio);
                List<Object[]> results = query.getResultList();

                // Consulta cuenta destino
                Query query1 = entityManager.createNativeQuery(sqlQuery1);
                query1.setParameter("id_persona", numSocio);
                query1.setParameter("cta_banco", numeroCtaDestino);
                List<Object[]> results1 = query1.getResultList();

                // Procesar resultados cuenta origen
                if (results.isEmpty()) {
                    response.put("message", "Cuenta origen no encontrada, no activa o no pertenece al socio perteneciente a esta cuenta!");
                    response.put("status", "ERROR004");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
                // Procesar resultados cuenta destino
                if (results1.isEmpty()) {
                    response.put("message", "Cuenta de destino no encontrada, no activa o no pertenece al socio perteneciente a esta cuenta!");
                    response.put("status", "ERROR005");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
                // Extraer datos de las cuentas
                Object[] resultEnvio = results.get(0);
                Object[] resultDestino = results1.get(0);

                String clieCodEmpresaEnvio = resultEnvio[0].toString().trim();
                String clienCodOficiEnvio = resultEnvio[1].toString().trim();
                String clienApellEnvio = resultEnvio[2].toString().trim();
                String clieNomEnvio = resultEnvio[3].toString().trim();
                String clienCodEnvio = resultEnvio[4].toString().trim();
                String clinIdenEnvio = resultEnvio[5].toString().trim();
                String nomApellido = clienApellEnvio +" " +clieNomEnvio;

                String clieIdBancoRecibe = resultDestino[0].toString().trim();
                String clieCtaBancoRecibe = resultDestino[1].toString().trim();
                String titulaCtaRecibe = resultDestino[2].toString().trim();
                String cedulaCtaRecibe = resultDestino[3].toString().trim();

                    String callSetLockProcedure = "CALL cnxprc_setea_lockm()";
                    Query lockProcedureQuery = entityManager.createNativeQuery(callSetLockProcedure);
                    lockProcedureQuery.executeUpdate();

                    String callTransferProcedure = "CALL cnxprc_reg_spi01_wb(:clienCodEmpreEnvio, :clienCodOficiEnvio,'803',:clienCodEmpreEnvio," +
                            ":clienCodOficiEnvio,:clienCodEnvio," +
                            ":clinIdenEnvio, :nomApellido,:numeroCuentaEnvio,:valTransferencia," +
                            ":cedulaCtaRecibe,:titulaCtaRecibe," +
                            ":clieIdBancoRecibe,:numeroCtaDestino,'1','TRANSFERENCIAS INTERBANCARIAS EN LINEA',1,'0.36')";
                    Query queryProcedure = entityManager.createNativeQuery(callTransferProcedure);

                    queryProcedure.setParameter("clienCodEmpreEnvio", clieCodEmpresaEnvio);
                    queryProcedure.setParameter("clienCodOficiEnvio", clienCodOficiEnvio);
                    queryProcedure.setParameter("clienCodEnvio",clienCodEnvio);
                    queryProcedure.setParameter("clinIdenEnvio", clinIdenEnvio);
                    queryProcedure.setParameter("nomApellido", nomApellido);
                    queryProcedure.setParameter("numeroCuentaEnvio", numeroCuentaEnvio);
                    queryProcedure.setParameter("valTransferencia", valTransferencia);
                    queryProcedure.setParameter("cedulaCtaRecibe",cedulaCtaRecibe);
                    queryProcedure.setParameter("titulaCtaRecibe",titulaCtaRecibe);
                    queryProcedure.setParameter("clieIdBancoRecibe",clieIdBancoRecibe);
                    queryProcedure.setParameter("numeroCtaDestino",numeroCtaDestino);
                    Object result = queryProcedure.getSingleResult();
                    int returnValue = Integer.parseInt(result.toString());
                    response.put("message", "TRANSFERENCIA INTERBANCARIA REALIZADA CON ÉXITO !!");
                    response.put("numTransferencia", returnValue);
                    response.put("status", "DTROK0005");
                    return new ResponseEntity<>(response, HttpStatus.OK);
            }
            response.put("message", "MONTO INSUFICIENTE PARA REALIZAR LA TRANSFERENCIA ");
            response.put("error", "ERROR105");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> srtGrabarPgTarjetas(HttpServletRequest token, TransferenciasDTO dto) {
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
            if (numeroCuentaEnvio == null || !numeroCuentaEnvio.matches("\\d{12}")) {
                response.put("message", "El número de cuenta origen debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (numeroCtaDestino == null || !numeroCtaDestino.matches("\\d+")) {
                response.put("message", "El número de cuenta destino debe contener únicamente dígitos numéricos.");
                response.put("status", "ERROR003");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (dto.getCodTempTransDirec() == null || !dto.getCodTempTransDirec().matches("\\d{6}")) {
                response.put("message", "Código de seguridad inválido");
                response.put("status", "AA023");
                response.put("error", "El código debe contener exactamente 6 dígitos");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (descripcionTrf == null || descripcionTrf.trim().isEmpty() || descripcionTrf.length() > 250) {
                response.put("message", "La descripción de la transferencia no puede estar vacía y debe tener como máximo 250 caracteres.");
                response.put("status", "ERROR004");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (valTransferencia == null || valTransferencia <= 0 || !valTransferencia.toString().matches("^\\d{1,14}(\\.\\d{1,2})?$")) {
                response.put("message", "El monto de la transferencia debe ser un número positivo con hasta 14 dígitos enteros y 2 decimales.");
                response.put("status", "ERROR005");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sqlVerificaTokenBDD = "SELECT codaccess_codigo_temporal FROM vircodaccess " +
                    "WHERE codaccess_cedula = :codaccess_cedula AND codaccess_usuario = :codaccess_usuario " +
                    "AND codaccess_estado = :codaccess_estado AND codsms_codigo = '6' ";
            Query queryVerificaTokenBDD = entityManager.createNativeQuery(sqlVerificaTokenBDD);
            queryVerificaTokenBDD.setParameter("codaccess_cedula", clienIdenti);
            queryVerificaTokenBDD.setParameter("codaccess_usuario", cliacUsuVirtu);
            queryVerificaTokenBDD.setParameter("codaccess_estado", "1");

            List<Object[]> resultsTokenBDD = queryVerificaTokenBDD.getResultList();
            if (resultsTokenBDD.isEmpty()) {
                response.put("message", "CODIGO TEMPORAL EXPIRADO, POR EXCEDER LOS 4 MINUTOS");
                response.put("status", "AA041");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String saldoDisponible = obtenerSaldoDisponible(numeroCuentaEnvio);
            System.out.println(saldoDisponible);
            Float saldoDispoParse = Float.parseFloat(saldoDisponible);

            String tokenFromDB = (String) queryVerificaTokenBDD.getSingleResult();
            if (!tokenFromDB.trim().equals(dto.getCodTempTransDirec())) {
                intentosRealizadoTokenFallosInterban++;
                if (intentosRealizadoTokenFallosInterban >= 3) {
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
                            Libs fechaHoraService = new Libs(entityManager);
                            String FechaHora = fechaHoraService.obtenerFechaYHora();

                            List<Object[]> results2 = resulDatosCorreoIngreso.getResultList();

                            for (Object[] row2 : results2) {
                                String clienApellidos = row2[0].toString().trim();
                                String clienNombres = row2[1].toString().trim();
                                String clienEmail = row2[2].toString().trim();
                                String IpIngreso = localIP();
                                sendEmail emailBloq = new sendEmail();
                                emailBloq.sendEmailBloqueo(clienApellidos, clienNombres, FechaHora, clienEmail, IpIngreso);
                            }

                            intentosRealizadoTokenFallosInterban = 0;
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
                    response.put("message", "Token incorrecto. Intentos restantes: " + (3 - intentosRealizadoTokenFallosInterban));
                    response.put("status", "AA023");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
            if (saldoDispoParse >= valTransferencia){
                String sqlQuery = """
                SELECT ctadp_cod_empre, ctadp_cod_ofici, clien_ape_clien, clien_nom_clien, ctadp_cod_clien, clien_ide_clien 
                FROM cnxctadp, cnxclien 
                WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp AND ctadp_cod_clien = :ctadp_cod_clien 
                AND ctadp_cod_ectad = '1' 
                AND ctadp_cod_clien = clien_cod_clien
                        """;
                String sqlQuery1 = """
                SELECT id_banco, cta_banco, titular, cedula
                FROM personas_transferencias  WHERE id_persona = :id_persona
                AND cta_banco = :cta_banco
                AND tipo_trf = 'E' AND vigente = 'T' AND tipo_prod_banc = 'TC'
                """;
                // Consulta cuenta origen
                Query query = entityManager.createNativeQuery(sqlQuery);
                query.setParameter("ctadp_cod_ctadp", numeroCuentaEnvio);
                query.setParameter("ctadp_cod_clien", numSocio);
                List<Object[]> results = query.getResultList();

                // Consulta cuenta destino
                Query query1 = entityManager.createNativeQuery(sqlQuery1);
                query1.setParameter("id_persona", numSocio);
                query1.setParameter("cta_banco", numeroCtaDestino);
                List<Object[]> results1 = query1.getResultList();

                // Procesar resultados cuenta origen
                if (results.isEmpty()) {
                    response.put("message", "Cuenta origen no encontrada, no activa o no pertenece al socio perteneciente a esta cuenta!");
                    response.put("status", "ERROR078");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
                // Procesar resultados cuenta destino
                if (results1.isEmpty()) {
                    response.put("message", "Tarjeta de debito, no activa o no pertenece al socio titular a esta cuenta!");
                    response.put("status", "ERROR058");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
                // Extraer datos de las cuentas
                Object[] resultEnvio = results.get(0);
                Object[] resultDestino = results1.get(0);

                String clieCodEmpresaEnvio = resultEnvio[0].toString().trim();
                String clienCodOficiEnvio = resultEnvio[1].toString().trim();
                String clienApellEnvio = resultEnvio[2].toString().trim();
                String clieNomEnvio = resultEnvio[3].toString().trim();
                String clienCodEnvio = resultEnvio[4].toString().trim();
                String clinIdenEnvio = resultEnvio[5].toString().trim();
                String nomApellido = clienApellEnvio +" " +clieNomEnvio;

                String clieIdBancoRecibe = resultDestino[0].toString().trim();
                String clieCtaBancoRecibe = resultDestino[1].toString().trim();
                String titulaCtaRecibe = resultDestino[2].toString().trim();
                String cedulaCtaRecibe = resultDestino[3].toString().trim();

                String callSetLockProcedure = "CALL cnxprc_setea_lockm()";
                Query lockProcedureQuery = entityManager.createNativeQuery(callSetLockProcedure);
                lockProcedureQuery.executeUpdate();

                String callTransferProcedure = "CALL cnxprc_reg_spi01_wb(:clienCodEmpreEnvio, :clienCodOficiEnvio,'803',:clienCodEmpreEnvio," +
                        ":clienCodOficiEnvio,:clienCodEnvio," +
                        ":clinIdenEnvio, :nomApellido,:numeroCuentaEnvio,:valTransferencia," +
                        ":cedulaCtaRecibe,:titulaCtaRecibe," +
                        ":clieIdBancoRecibe,:numeroCtaDestino,'4','PAGO DE TARJETA DE CREDITO',1,'0.36')";
                Query queryProcedure = entityManager.createNativeQuery(callTransferProcedure);

                queryProcedure.setParameter("clienCodEmpreEnvio", clieCodEmpresaEnvio);
                queryProcedure.setParameter("clienCodOficiEnvio", clienCodOficiEnvio);
                queryProcedure.setParameter("clienCodEnvio",clienCodEnvio);
                queryProcedure.setParameter("clinIdenEnvio", clinIdenEnvio);
                queryProcedure.setParameter("nomApellido", nomApellido);
                queryProcedure.setParameter("numeroCuentaEnvio", numeroCuentaEnvio);
                queryProcedure.setParameter("valTransferencia", valTransferencia);
                queryProcedure.setParameter("cedulaCtaRecibe",cedulaCtaRecibe);
                queryProcedure.setParameter("titulaCtaRecibe",titulaCtaRecibe);
                queryProcedure.setParameter("clieIdBancoRecibe",clieIdBancoRecibe);
                queryProcedure.setParameter("numeroCtaDestino",numeroCtaDestino);
                Object result = queryProcedure.getSingleResult();
                int returnValue = Integer.parseInt(result.toString());
                response.put("message", "PAGO DE TARJETA REALIZADO CON ÉXITO !!");
                response.put("numTransferencia", returnValue);
                response.put("status", "DTROK0005");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            response.put("message", "MONTO INSUFICIENTE PARA REALIZAR EL PAGO DE TARJETA ");
            response.put("error", "ERROR105");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, Object>> genCodDirectas(HttpServletRequest token, TransferenciasDTO dto) {
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            Map<String, Object> response = new HashMap<>();
            String numeroCuentaEnvio = dto.getCtaEnvio();
            String numeroCtaDestino = dto.getCtaDestino();

            // Validación de datos del token
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                response.put("message", "Datos del token incompletos");
                response.put("status", "AA022");
                response.put("error", "ERROR EN LA AUTENTICACIÓN");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

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
            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFechaYHora();
            System.out.println(fecha);

            String sqlQueryOrigen = "SELECT clien_cod_empre, clien_cod_ofici, ctadp_cod_ctadp, clien_tlf_celul, clien_dir_email, clien_nom_clien, clien_ape_clien " +
                    "FROM cnxctadp, cnxclien, cnxcliac " +
                    "WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp " +
                    "AND ctadp_cod_ectad = :ctadp_cod_ectad " +
                    "AND ctadp_cod_clien = :clien_cod_clien " +
                    "AND clien_ide_clien = :clien_ide_clien " +
                    "AND ctadp_cod_clien = clien_cod_clien " +
                    "AND clien_ide_clien = cliac_ide_clien ";

            // Consulta cuenta origen
            Query query = entityManager.createNativeQuery(sqlQueryOrigen);
            query.setParameter("ctadp_cod_ctadp", numeroCuentaEnvio);
            query.setParameter("ctadp_cod_ectad", "1");
            query.setParameter("clien_cod_clien", numSocio);
            query.setParameter("clien_ide_clien",clienIdenti);
            List<Object[]> results = query.getResultList();

            String sqlQueryVerDestino = """
                    SELECT * FROM cnxctadp WHERE ctadp_cod_ctadp = :cta_banco
                    """;

            // Consulta cuenta destino
            Query query1 = entityManager.createNativeQuery(sqlQueryVerDestino);
            query1.setParameter("cta_banco", numeroCtaDestino);
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
            String tlfCtaEnvio = resultEnvio[3].toString().trim();
            String emailCtaEnvio = resultEnvio[4].toString().trim();
            String nombreCtaEnvio = resultEnvio[5].toString().trim();
            String apellCtaEnvio = resultEnvio[6].toString().trim();

            String clienCodEmpreDestino = resultDestino[0].toString().trim();
            String clienCodOficiDestino = resultDestino[1].toString().trim();
            String ctadpCodCtadpDestino = resultDestino[2].toString().trim();
            String tlfCtaDestino = resultEnvio[3].toString().trim();
            String emailCtaDestino = resultEnvio[4].toString().trim();

            //generar codigo
            String CodigoTrfDirectas = codigoAleatorio6Temp();
            SendSMS smsDesbloqueo = new SendSMS();
            smsDesbloqueo.sendSecurityCodeSMS(tlfCtaEnvio,"1150",CodigoTrfDirectas,"efectuar la Transferencia directa", fecha);
            // Enviar correo
            sendEmail enviarCorreo = new sendEmail();
            enviarCorreo.sendEmailTokenTemp(apellCtaEnvio, nombreCtaEnvio, fecha, emailCtaEnvio, CodigoTrfDirectas);

            // Actualizar estados anteriores a 0
            String sqlUpdateEstado = "UPDATE vircodaccess SET codaccess_estado = '0' WHERE codaccess_cedula = :codaccess_cedula AND codaccess_estado = '1' AND codsms_codigo = 3";
            Query resultUpdateEstado = entityManager.createNativeQuery(sqlUpdateEstado);
            resultUpdateEstado.setParameter("codaccess_cedula", clienIdenti);
            resultUpdateEstado.executeUpdate();

            // Insertar nuevo código temporal
            String sqlInsertToken = "INSERT INTO vircodaccess (codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal, codsms_codigo, codaccess_estado, codaccess_fecha) " +
                    "VALUES (:codaccess_cedula, :codaccess_usuario, :codaccess_codigo_temporal, :codsms_codigo, :codaccess_estado, :codaccess_fecha)";
            Query resultInsertTokenAcceso = entityManager.createNativeQuery(sqlInsertToken);
            resultInsertTokenAcceso.setParameter("codaccess_cedula", clienIdenti);
            resultInsertTokenAcceso.setParameter("codaccess_usuario", cliacUsuVirtu);
            resultInsertTokenAcceso.setParameter("codaccess_codigo_temporal", CodigoTrfDirectas);
            resultInsertTokenAcceso.setParameter("codsms_codigo", 3);
            resultInsertTokenAcceso.setParameter("codaccess_estado", "1");
            resultInsertTokenAcceso.setParameter("codaccess_fecha", fecha);
            resultInsertTokenAcceso.executeUpdate();
            tokenExpirationService.programarExpiracionToken(clienIdenti, CodigoTrfDirectas, "3");

            response.put("message", "CODIGO GENERADO CON EXITO :)");
            response.put("status", "CODTRFOK005");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> genCodInterbancarias(HttpServletRequest token, TransferenciasDTO dto) {
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            Map<String, Object> response = new HashMap<>();
            String numeroCuentaEnvio = dto.getCtaEnvio();
            String numeroCtaDestino = dto.getCtaDestino();

            // Validación de datos del token
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                response.put("message", "Datos del token incompletos");
                response.put("status", "AA022");
                response.put("error", "ERROR EN LA AUTENTICACIÓN");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Validación de cuenta origen
            if (numeroCuentaEnvio == null || !numeroCuentaEnvio.matches("\\d{12}")) {
                response.put("message", "El número de cuenta origen debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFechaYHora();
            System.out.println(fecha);
            String sqlQuery = "SELECT clien_cod_empre, clien_cod_ofici, ctadp_cod_ctadp, clien_tlf_celul, clien_dir_email, clien_nom_clien, clien_ape_clien " +
                    "FROM cnxctadp, cnxclien, cnxcliac " +
                    "WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp " +
                    "AND ctadp_cod_depos = :ctadp_cod_depos " +
                    "AND ctadp_cod_ectad = :ctadp_cod_ectad " +
                    "AND ctadp_cod_clien = clien_cod_clien " +
                    "AND clien_ide_clien = cliac_ide_clien";
            String verInfoInter = """
                    SELECT * FROM personas_transferencias WHERE id_persona = :numSocio AND cta_banco = :ctadp_cod_ctadp
                    """;
            // Consulta cuenta origen
            Query query = entityManager.createNativeQuery(sqlQuery);
            query.setParameter("ctadp_cod_ctadp", numeroCuentaEnvio);
            query.setParameter("ctadp_cod_depos", "1");
            query.setParameter("ctadp_cod_ectad", "1");
            List<Object[]> results = query.getResultList();

            // Consulta cuenta destino
            Query query1 = entityManager.createNativeQuery(verInfoInter);
            query1.setParameter("ctadp_cod_ctadp", numeroCtaDestino);
            query1.setParameter("numSocio",numSocio);
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
            String tlfCtaEnvio = resultEnvio[3].toString().trim();
            String emailCtaEnvio = resultEnvio[4].toString().trim();
            String nombreCtaEnvio = resultEnvio[5].toString().trim();
            String apellCtaEnvio = resultEnvio[6].toString().trim();

            String clienCodEmpreDestino = resultDestino[0].toString().trim();
            String clienCodOficiDestino = resultDestino[1].toString().trim();
            String ctadpCodCtadpDestino = resultDestino[2].toString().trim();
            String tlfCtaDestino = resultEnvio[3].toString().trim();
            String emailCtaDestino = resultEnvio[4].toString().trim();

            //generar codigo
            String CodigoTrfDirectas = codigoAleatorio6Temp();
            SendSMS smsDesbloqueo = new SendSMS();
            smsDesbloqueo.sendSecurityCodeSMS(tlfCtaEnvio,"1150",CodigoTrfDirectas,"efectuar la Transferencia interbancaria", fecha);
            // Enviar correo
            sendEmail enviarCorreo = new sendEmail();
            enviarCorreo.sendEmailTokenTemp(apellCtaEnvio, nombreCtaEnvio, fecha, emailCtaEnvio, CodigoTrfDirectas);

            // Actualizar estados anteriores a 0
            String sqlUpdateEstado = "UPDATE vircodaccess SET codaccess_estado = '0' WHERE codaccess_cedula = :codaccess_cedula AND codaccess_estado = '1' AND codsms_codigo = 2";
            Query resultUpdateEstado = entityManager.createNativeQuery(sqlUpdateEstado);
            resultUpdateEstado.setParameter("codaccess_cedula", clienIdenti);
            resultUpdateEstado.executeUpdate();

            // Insertar nuevo código temporal
            String sqlInsertToken = "INSERT INTO vircodaccess (codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal, codsms_codigo, codaccess_estado, codaccess_fecha) " +
                    "VALUES (:codaccess_cedula, :codaccess_usuario, :codaccess_codigo_temporal, :codsms_codigo, :codaccess_estado, :codaccess_fecha)";
            Query resultInsertTokenAcceso = entityManager.createNativeQuery(sqlInsertToken);
            resultInsertTokenAcceso.setParameter("codaccess_cedula", clienIdenti);
            resultInsertTokenAcceso.setParameter("codaccess_usuario", cliacUsuVirtu);
            resultInsertTokenAcceso.setParameter("codaccess_codigo_temporal", CodigoTrfDirectas);
            resultInsertTokenAcceso.setParameter("codsms_codigo", 2);
            resultInsertTokenAcceso.setParameter("codaccess_estado", "1");
            resultInsertTokenAcceso.setParameter("codaccess_fecha", fecha);
            resultInsertTokenAcceso.executeUpdate();
            tokenExpirationService.programarExpiracionToken(clienIdenti, CodigoTrfDirectas, "2");

            response.put("message", "CODIGO GENERADO CON EXITO :)");
            response.put("status", "CODTRFOK005");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error interno del servidor");
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public ResponseEntity<Map<String, Object>> genCodInterbancarias_PgTj(HttpServletRequest token, TransferenciasDTO dto) {
        try {
            String cliacUsuVirtu = (String) token.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) token.getAttribute("ClienIdenti");
            String numSocio = (String) token.getAttribute("numSocio");
            Map<String, Object> response = new HashMap<>();
            String numeroCuentaEnvio = dto.getCtaEnvio();
            String numeroCtaDestino = dto.getCtaDestino();

            // Validación de datos del token
            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                response.put("message", "Datos del token incompletos");
                response.put("status", "AA022");
                response.put("error", "ERROR EN LA AUTENTICACIÓN");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Validación de cuenta origen
            if (numeroCuentaEnvio == null || !numeroCuentaEnvio.matches("\\d{12}")) {
                response.put("message", "El número de cuenta origen debe tener exactamente 12 dígitos numéricos.");
                response.put("status", "ERROR002");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            Libs fechaHoraService = new Libs(entityManager);
            String fecha = fechaHoraService.obtenerFechaYHora();
            System.out.println(fecha);
            String sqlQuery = "SELECT clien_cod_empre, clien_cod_ofici, ctadp_cod_ctadp, clien_tlf_celul, clien_dir_email, clien_nom_clien, clien_ape_clien " +
                    "FROM cnxctadp, cnxclien, cnxcliac " +
                    "WHERE ctadp_cod_ctadp = :ctadp_cod_ctadp " +
                    "AND ctadp_cod_depos = :ctadp_cod_depos " +
                    "AND ctadp_cod_ectad = :ctadp_cod_ectad " +
                    "AND ctadp_cod_clien = clien_cod_clien " +
                    "AND clien_ide_clien = cliac_ide_clien";
            String verInfoInter = """
                    SELECT * FROM personas_transferencias WHERE id_persona = :numSocio AND cta_banco = :ctadp_cod_ctadp
                    """;
            // Consulta cuenta origen
            Query query = entityManager.createNativeQuery(sqlQuery);
            query.setParameter("ctadp_cod_ctadp", numeroCuentaEnvio);
            query.setParameter("ctadp_cod_depos", "1");
            query.setParameter("ctadp_cod_ectad", "1");
            List<Object[]> results = query.getResultList();

            // Consulta cuenta destino
            Query query1 = entityManager.createNativeQuery(verInfoInter);
            query1.setParameter("ctadp_cod_ctadp", numeroCtaDestino);
            query1.setParameter("numSocio",numSocio);
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
            String tlfCtaEnvio = resultEnvio[3].toString().trim();
            String emailCtaEnvio = resultEnvio[4].toString().trim();
            String nombreCtaEnvio = resultEnvio[5].toString().trim();
            String apellCtaEnvio = resultEnvio[6].toString().trim();

            //generar codigo
            String CodigoTrfDirectas = codigoAleatorio6Temp();
            SendSMS smsDesbloqueo = new SendSMS();
            smsDesbloqueo.sendSecurityCodeSMS(tlfCtaEnvio,"1150",CodigoTrfDirectas,"efectuar el pago de su tarjeta", fecha);
            // Enviar correo
            sendEmail enviarCorreo = new sendEmail();
            enviarCorreo.sendEmailTokenTemp(apellCtaEnvio, nombreCtaEnvio, fecha, emailCtaEnvio, CodigoTrfDirectas);

            // Actualizar estados anteriores a 0
            String sqlUpdateEstado = "UPDATE vircodaccess SET codaccess_estado = '0' WHERE codaccess_cedula = :codaccess_cedula AND codaccess_estado = '1' AND codsms_codigo = 6";
            Query resultUpdateEstado = entityManager.createNativeQuery(sqlUpdateEstado);
            resultUpdateEstado.setParameter("codaccess_cedula", clienIdenti);
            resultUpdateEstado.executeUpdate();

            // Insertar nuevo código temporal
            String sqlInsertToken = "INSERT INTO vircodaccess (codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal, codsms_codigo, codaccess_estado, codaccess_fecha) " +
                    "VALUES (:codaccess_cedula, :codaccess_usuario, :codaccess_codigo_temporal, :codsms_codigo, :codaccess_estado, :codaccess_fecha)";
            Query resultInsertTokenAcceso = entityManager.createNativeQuery(sqlInsertToken);
            resultInsertTokenAcceso.setParameter("codaccess_cedula", clienIdenti);
            resultInsertTokenAcceso.setParameter("codaccess_usuario", cliacUsuVirtu);
            resultInsertTokenAcceso.setParameter("codaccess_codigo_temporal", CodigoTrfDirectas);
            resultInsertTokenAcceso.setParameter("codsms_codigo", 6);
            resultInsertTokenAcceso.setParameter("codaccess_estado", "1");
            resultInsertTokenAcceso.setParameter("codaccess_fecha", fecha);
            resultInsertTokenAcceso.executeUpdate();
            tokenExpirationService.programarExpiracionToken(clienIdenti, CodigoTrfDirectas, "6");

            response.put("message", "CODIGO GENERADO CON EXITO :)");
            response.put("status", "CODTRFOK005");
            return new ResponseEntity<>(response, HttpStatus.OK);

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
    public static String localIP() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "No disponible";
        }
    }
    public String codigoAleatorio6Temp() {
        // Genera un número aleatorio de 6 dígitos
        Random random = new Random();
        int numeroAleatorio = 100000 + random.nextInt(900000); // Asegura 6 dígitos
        return String.valueOf(numeroAleatorio);
    }

}
