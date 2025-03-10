package com.ApiVirtualT.ApiVirtual.apiAutenticacion.services;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.JWT.JwtUtil;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.UserCredentials;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import com.ApiVirtualT.ApiVirtual.libs.Libs;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;

import libs.PassSecure;
import envioCorreo.sendEmail;
import sms.SendSMS;
import org.springframework.transaction.annotation.Transactional;


@Transactional
@Service
@RequiredArgsConstructor
public class AuthService {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TokenExpirationService tokenExpirationService;

    /**
     * Funcion para validar login antes de consultar bdd
        */

    private int intentosRealizados = 0, intentosRealizadoTokenFallos = 0;
    public ResponseEntity<Map<String, Object>> accesslogin( UserCredentials  request) {
        try {
        Map<String, Object> allData = new HashMap<>();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> allDataList = new ArrayList<>();
        HttpStatus status = HttpStatus.OK;

        String mensajeValBlancos = validarCredencialesBlanco(request);
        if (mensajeValBlancos != null) {
            allData.put("message", mensajeValBlancos);
            allData.put("status", "AA01");
            allData.put("errors", "No se puede enviar campos con espacios en blanco. ");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        String mensajeUsarioNoSerCorreo = usarioNoSerCorreo(request);
        if (mensajeUsarioNoSerCorreo != null) {
            allData.put("message", mensajeUsarioNoSerCorreo);
            allData.put("status", "AA02");
            allData.put("errors", "No se acepta correos electronicos en el usuario. ");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (request.getCliacUsuVirtu() != null && request.getCliacUsuVirtu().contains("ñ")) {
            allData.put("message", "Usuario inválido");
            allData.put("status", "AA03");
            allData.put("errors", "No se permite el carácter 'ñ' en el usuario");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (request.getClienWwwPswrd() != null && request.getClienWwwPswrd().contains("ñ")) {
            allData.put("message", "Contraseña inválido");
            allData.put("status", "AA04");
            allData.put("errors", "No se permite el carácter 'ñ' en la password");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        List<Object[]> resultados =valida_usuario_id(request.getCliacUsuVirtu());

        if (resultados.isEmpty()) {

            String usuario = request.getCliacUsuVirtu();
            String contraUser = request.getClienWwwPswrd();
            Map<String, Object> validacion = valida_LoginBDD(usuario, contraUser);
            if (Boolean.TRUE.equals(validacion.get("success"))) {
                allData.put("message", "Acceso concedido.");
                allData.put("status", "AA00");
                String token = (String) validacion.get("token");
                allData.put("token", token);
                status = HttpStatus.OK;
            } else if (Boolean.TRUE.equals(validacion.get("cod4digitos"))) {
                allData.put("message", "Debe cambiar su contraseña o es una contraseña temporal.");
                allData.put("status", "AA06");
                allData.put("errors", "Usuario con contraseña temporal.");
                allData.put("token", validacion.get("token"));
                status = HttpStatus.BAD_REQUEST;
            } else {
                allData.put("message", validacion.get("message"));
                allData.put("status", validacion.get("status"));
                allData.put("errors", validacion.get("errors"));
                status = HttpStatus.BAD_REQUEST;
            }
            allDataList.add(allData);
            response.put("AllData", allDataList);
        }else{
            allData.put("message", "Por favor, no utilice la identificación como si fuera un nombre de usuario.");
            allData.put("status", "AA017");
            allData.put("errors", "Se debe utilizar el nombre de usuario en lugar de la cédula de identificación.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(response, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<Map<String, Object>> validarCodSeguridad(HttpServletRequest request, CodSegurdiad codSeguridad) {
        try {
            // Obtener los valores guardados en el request por el filtro JWT
            String cliacUsuVirtu = (String) request.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) request.getAttribute("ClienIdenti");
            String numSocio = (String) request.getAttribute("numSocio");

            Map<String, Object> allData = new HashMap<>();
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> allDataList = new ArrayList<>();
            HttpStatus status = HttpStatus.OK;


            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                allData.put("message", "Datos del token incompletos");
                allData.put("status", "AA022");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            if (codSeguridad.getCodaccess_codigo_temporal() == null || !codSeguridad.getCodaccess_codigo_temporal().matches("\\d{6}")) {
                allData.put("message", "Código de seguridad inválido");
                allData.put("status", "AA023");
                allData.put("errors", "El código debe contener exactamente 6 dígitos");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String mensajeValidarCodigoSeguridad = validarCodigoSeguridad(codSeguridad);
            if (mensajeValidarCodigoSeguridad != null) {
                allData.put("message", mensajeValidarCodigoSeguridad);
                allData.put("status", "AA021");
                allData.put("errors", "ERROR EN EL CÓDIGO DE SEGURIDAD");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String sqlVerificaTokenBDD = "SELECT codaccess_codigo_temporal FROM vircodaccess " +
                    "WHERE codaccess_cedula = :codaccess_cedula AND codaccess_usuario = :codaccess_usuario AND codaccess_estado = :codaccess_estado ";
            Query queryVerificaTokenBDD = entityManager.createNativeQuery(sqlVerificaTokenBDD);
            queryVerificaTokenBDD.setParameter("codaccess_cedula", clienIdenti);
            queryVerificaTokenBDD.setParameter("codaccess_usuario", cliacUsuVirtu);
            queryVerificaTokenBDD.setParameter("codaccess_estado", "1");
            List<Object[]> resultsTokenBDD = queryVerificaTokenBDD.getResultList();
            if (!resultsTokenBDD.isEmpty()) {
                String tokenFromDB = (String) queryVerificaTokenBDD.getSingleResult();
                            if (tokenFromDB != null && codSeguridad.getCodaccess_codigo_temporal() != null &&
                                    codSeguridad.getCodaccess_codigo_temporal().equals(tokenFromDB.trim())) {
                                String sqlDatosCorreoIngreso = "SELECT clien_ape_clien, clien_nom_clien, clien_dir_email, clien_tlf_celul, clien_ide_clien, clien_cod_clien FROM cnxclien, cnxcliac " +
                                        "WHERE cliac_usu_virtu = :username AND clien_ide_clien = cliac_ide_clien";
                                Query resulDatosCorreoIngreso = entityManager.createNativeQuery(sqlDatosCorreoIngreso);
                                resulDatosCorreoIngreso.setParameter("username", cliacUsuVirtu);

                                List<Object[]> results2 = resulDatosCorreoIngreso.getResultList();
                                for (Object[] row2 : results2) {
                                    String clienApellidos = row2[0].toString().trim();
                                    String clienNombres = row2[1].toString().trim();
                                    String clienEmail = row2[2].toString().trim();
                                    String clienNumero = row2[3].toString().trim();
                                    System.out.println("Consulta BDD= APELLIDOS: " + clienApellidos + " NOMBRES: " + clienNombres + " EMAIL: " + clienEmail + " CELULAR " + clienNumero);
                                    String IpIngresoLogin = localIP();

                                    Libs fechaHoraService = new Libs(entityManager);
                                    String FechaIngresoLogin = fechaHoraService.obtenerFechaYHora();
                                    System.out.println(FechaIngresoLogin);

                                    SendSMS sms = new SendSMS();
                                    sms.sendVirtualAccessSMS(clienNumero, "1150", "VIRTUALCOP",FechaIngresoLogin);
                                    sendEmail enviarCorreo = new sendEmail();
                                    enviarCorreo.sendEmailInicioSesion(clienApellidos, clienNombres, FechaIngresoLogin, IpIngresoLogin, clienEmail);
                                    allData.put("status", "AUTHO");
                                    allData.put("message", "Inicio de sesion exitoso!");
                                    allDataList.add(allData);
                                    response.put("AllData", allDataList);
                                    return new ResponseEntity<>(response, HttpStatus.OK);
                                }

                                intentosRealizadoTokenFallos = 0;
                            }else{
                                    intentosRealizadoTokenFallos++;
                                    if (intentosRealizadoTokenFallos >= 3) {
                                    String sqlBloqUser = "UPDATE cnxcliac SET cliac_ctr_bloq = :bloqueo WHERE cliac_usu_virtu = :username";
                                    Query resultBloqUser = entityManager.createNativeQuery(sqlBloqUser);
                                    resultBloqUser.setParameter("bloqueo", "0");
                                    resultBloqUser.setParameter("username", cliacUsuVirtu);

                                        try {
                                            int rowsUpdated = resultBloqUser.executeUpdate();
                                            if (rowsUpdated > 0) {
                                                String sqlDatosCorreoIngreso = "SELECT clien_ape_clien, clien_nom_clien, clien_dir_email FROM cnxclien, cnxcliac " +
                                                        "WHERE cliac_usu_virtu = :username AND clien_ide_clien = cliac_ide_clien";
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
                                                    emailBloq.sendEmailBloqueo(clienApellidos, clienNombres, FechaHora,clienEmail, IpIngreso );
                                                intentosRealizadoTokenFallos = 0;
                                                response.put("success", false);
                                                response.put("message", "Usuario bloqueado por exceder límite de intentos");
                                                response.put("status", "AA025");
                                                status = HttpStatus.BAD_REQUEST;
                                                }
                                            }
                                        } catch (Exception e) {
                                            response.put("success", false);
                                            response.put("message", "Error al intentar bloquear el usuario");
                                            response.put("status", "AA024");
                                            status = HttpStatus.BAD_REQUEST;
                                        }
                                    } else {
                                        response.put("success", false);
                                        response.put("message", "Token incorrecto. Intentos restantes: " + (4 - intentosRealizadoTokenFallos));
                                        response.put("status", "AA023");
                                        status = HttpStatus.BAD_REQUEST;

                                    }
                            }
            }else {
                allData.put("status", "AA027");
                allData.put("errors", "CODIGO TEMPORAL EXPIRADO, POR EXCEDER LOS 4 MINUTOS");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            return new ResponseEntity<>(response, status);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> errorData = new HashMap<>();
            List<Map<String, Object>> errorList = new ArrayList<>();

            errorData.put("message", "Error interno del servidor");
            errorData.put("status", "ERROR");
            errorData.put("errors", e.getMessage());
            errorList.add(errorData);
            errorResponse.put("AllData", errorList);

            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String validarCodigoSeguridad(CodSegurdiad request) {

        if (request.getCodaccess_codigo_temporal() == null || request.getCodaccess_codigo_temporal().trim().isEmpty()) {
            return "El código temporal no puede estar vacío o contener solo espacios.";
        }

        if (request.getCodaccess_codigo_temporal().length() < 6) {
            return "El código temporal debe tener al menos 6 caracteres.";
        }

        return null;
    }

    public String usarioNoSerCorreo(UserCredentials request){
        String regexCorreo = "^[\\w-]+(?:\\.[\\w-]+)*@[\\w-]+(?:\\.[\\w-]+)+$";
        String usuario = request.getCliacUsuVirtu();

        if (usuario.matches(regexCorreo)) {
            return "El nombre de usuario no puede ser un correo electrónico.";
        }
        return null;
    }

    public String validarCredencialesBlanco(UserCredentials request) {
        if (request.getCliacUsuVirtu() == null || request.getCliacUsuVirtu().isEmpty()) {

            return "El nombre de usuario no puede estar vacío.";
        }

        if (request.getClienWwwPswrd() == null || request.getClienWwwPswrd().isEmpty()) {
            return "La contraseña no puede estar vacía.";
        }
        return null;
    }

    public List<Object[]> valida_usuario_id(String user) {

        String sql = "SELECT cliac_usu_virtu,clien_www_pswrd FROM cnxcliac,cnxclien" +
                "               WHERE cliac_ide_clien=:username" +
                "               AND cliac_ide_clien=clien_ide_clien";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("username", user);
        List<Object[]> results = query.getResultList();

        return results;
    }

/**
    * Funcion para validar Login con LA BDD
        */

public Map<String, Object> valida_LoginBDD(String user, String password) {
    Map<String, Object> response = new HashMap<>();
    try {
        // Consulta para verificar usuario y contraseña
        String sql = "SELECT cliac_usu_virtu, clien_www_pswrd FROM cnxcliac, cnxclien " +
                "WHERE cliac_usu_virtu = :username AND cliac_ide_clien = clien_ide_clien";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("username", user);
        List<Object[]> results = query.getResultList();
        if (results.isEmpty()) {
            response.put("success", false);
            response.put("message", "El usuario no existe o las credenciales son incorrectas.");
            response.put("status", "AA05");
            response.put("errors", "Usuario no encontrado.");
            return response;
        }
        // Consulta para verificar si el usuario está bloqueado
        String sqlBloq = "SELECT cliac_ctr_bloq, clien_ctr_estad FROM cnxcliac JOIN cnxclien ON cliac_ide_clien=clien_ide_clien WHERE cliac_usu_virtu = :username";
        Query resultSQLBloq = entityManager.createNativeQuery(sqlBloq);
        resultSQLBloq.setParameter("username", user);
        List<Object[]> results1 = resultSQLBloq.getResultList();
        // Validación de las credenciales
        for (Object[] row : results) {
            String cliacUsuVirtu = (String) row[0];
            String clienWwwPswrd = (String) row[1];
            for (Object[] row1 : results1) {
                String cliacBloq = row1[0].toString();
                String clien_estado = row1[1].toString();
                System.out.println("Consulta BDD= Bloq: " + cliacBloq + ", Estado: " + clien_estado);
            // Limpieza de datos
            clienWwwPswrd = clienWwwPswrd.trim();
            if (clienWwwPswrd.startsWith("\"") && clienWwwPswrd.endsWith("\"")) {
                clienWwwPswrd = clienWwwPswrd.substring(1, clienWwwPswrd.length() - 1).trim();
            }
            PassSecure passSecure = new PassSecure();
            String passDec = passSecure.decryptPassword(clienWwwPswrd);
            passDec = passDec.trim();
            if (passDec.startsWith("\"") && passDec.endsWith("\"")) {
                passDec = passDec.substring(1, passDec.length() - 1).trim();
            }
                String sqlDatosInfoToken = "SELECT clien_ide_clien, clien_cod_clien FROM cnxclien, cnxcliac " +
                        "WHERE cliac_usu_virtu = :username AND clien_ide_clien = cliac_ide_clien";
                Query resulDatosInfoToken = entityManager.createNativeQuery(sqlDatosInfoToken);
                resulDatosInfoToken.setParameter("username", user);
                List<Object[]> results3 = resulDatosInfoToken.getResultList();
                // Verificar si hay resultados
                if (results3.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "No se encontraron datos para el usuario");
                    return response;
                }
                // Procesar el primer resultado
                Object[] row10 = results3.get(0);
                String clienCedula1 = row10[0].toString().trim();
                String clienCodClie1 = row10[1].toString().trim();
                String token1 = JwtUtil.generateToken(user, clienCedula1, clienCodClie1);

                if(password.length() <= 4){
                    System.out.println(clienCedula1);
                    System.out.println(clienCodClie1);
                    System.out.println(token1);
                    response.put("cod4digitos", true);
                    response.put("message", "Debe cambiar su contraseña o es contraseña temporal.");
                    response.put("status", "AA06");
                    response.put("errors", "Usuario con contraseña temporal.");
                    response.put("token", token1);
                    return response;
                }
                if (cliacUsuVirtu.equals(user) && passDec.equals(password)) {
                    intentosRealizados = 0;
                    if (!results1.isEmpty()) {
                        if ("0".equals(cliacBloq.trim()) || "0".equals(clien_estado.trim())) {
                            response.put("success", false);
                            response.put("message", "El usuario está bloqueado o no está activo.");
                            response.put("status", "AA07");
                            response.put("errors", "Usuario bloqueado.");
                            return response;
                        }
                        else{
                            String sqlDatosCorreo = "SELECT clien_ape_clien, clien_nom_clien, clien_dir_email, clien_tlf_celul, clien_ide_clien, clien_cod_clien FROM cnxclien, cnxcliac " +
                                    "WHERE cliac_usu_virtu = :username AND clien_ide_clien = cliac_ide_clien";
                            Query resulDatosCorreo = entityManager.createNativeQuery(sqlDatosCorreo);
                            resulDatosCorreo.setParameter("username", user);

                            List<Object[]> results2 = resulDatosCorreo.getResultList();
                            for (Object[] row2 : results2) {
                                String clienApellidos = row2[0].toString().trim();
                                String clienNombres = row2[1].toString().trim();
                                String clienEmail = row2[2].toString().trim();
                                String clienNumero = row2[3].toString().trim();
                                String clienCedula = row2[4].toString().trim();
                                String clienCodClie = row2[5].toString().trim();
                                System.out.println("Consulta BDD= APELLIDOS: " + clienApellidos + " NOMBRES: " + clienNombres + " EMAIL: " + clienEmail + " CELULAR " + clienNumero);
                                Libs fechaHoraService = new Libs(entityManager);

                                String FechaIngresoLogin = fechaHoraService.obtenerFechaYHora();
                                System.out.println(FechaIngresoLogin);

                                String tokenTemp = codigoAleatorioTemp();

                                    SendSMS smsCodigoTemp = new SendSMS();
                                    smsCodigoTemp.sendSecurityCodeSMS(clienNumero,"1150", tokenTemp, "Iniciar Sesion",FechaIngresoLogin);
                                    sendEmail enviaCorreoToken = new sendEmail();

                                    enviaCorreoToken.sendEmailTokenTemp(clienApellidos, clienNombres, FechaIngresoLogin, clienEmail, tokenTemp);
                                    String sqlUpdateEstado = "UPDATE vircodaccess SET codaccess_estado = '0' WHERE codaccess_cedula = :codaccess_cedula AND codaccess_estado = '1'";

                                    Query resultUpdateEstado = entityManager.createNativeQuery(sqlUpdateEstado);
                                    resultUpdateEstado.setParameter("codaccess_cedula", clienCedula);  // o cliacUsuVirtu, dependiendo de qué campo estés usando para identificar al usuario
                                    resultUpdateEstado.executeUpdate();

                                    String sqlInsertToken = "INSERT INTO vircodaccess (codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal, codsms_codigo, codaccess_estado, codaccess_fecha) VALUES (:codaccess_cedula, :codaccess_usuario, :codaccess_codigo_temporal, :codsms_codigo, :codaccess_estado, :codaccess_fecha)";
                                    Query resultInsertTokenAcceso = entityManager.createNativeQuery(sqlInsertToken);
                                    resultInsertTokenAcceso.setParameter("codaccess_cedula", clienCedula);
                                    resultInsertTokenAcceso.setParameter("codaccess_usuario", cliacUsuVirtu);
                                    resultInsertTokenAcceso.setParameter("codaccess_codigo_temporal", tokenTemp);
                                    resultInsertTokenAcceso.setParameter("codsms_codigo", 1);
                                    resultInsertTokenAcceso.setParameter("codaccess_estado", "1");
                                    resultInsertTokenAcceso.setParameter("codaccess_fecha", FechaIngresoLogin);
                                    resultInsertTokenAcceso.executeUpdate();
                                    tokenExpirationService.programarExpiracionToken(clienCedula, tokenTemp, "1");

                                    String token = JwtUtil.generateToken(cliacUsuVirtu, clienCedula, clienCodClie);
                                    response.put("success", true);
                                    response.put("token", token);
                                }
                        }
                    }
                    response.put("success", true);
                    response.put("message", "Inicio de sesión exitoso.");
                    return response;
                } else {
                    intentosRealizados++;
                    System.out.println("Intentos realizados = " + intentosRealizados);
                    if(intentosRealizados >= 3) {
                        String sqlBloqUser = "UPDATE cnxcliac SET cliac_ctr_bloq = :bloqueo WHERE cliac_usu_virtu = :username";
                        Query resultBloqUser = entityManager.createNativeQuery(sqlBloqUser);
                        resultBloqUser.setParameter("bloqueo", "0");
                        resultBloqUser.setParameter("username", cliacUsuVirtu);
                        //MANDAR CORREO DE BLOQUEO
                        try {
                            // Ejecutar la actualización
                            int rowsUpdated = resultBloqUser.executeUpdate();
                            if (rowsUpdated > 0) {
                                System.out.println("Usuario bloqueado exitosamente en la base de datos.");
                                intentosRealizados = 0;
                            } else {
                                System.out.println("No se encontró al usuario para bloquear.");
                            }
                        } catch (Exception e) {
                            System.err.println("Error al bloquear el usuario en la base de datos: " + e.getMessage());
                            response.put("success", false);
                            response.put("message", "Error al intentar bloquear el usuario.");
                            response.put("status", "AA10");
                            response.put("errors", e.getMessage());
                            return response;
                        }
                        response.put("success", false);
                        response.put("message", "Se alcanzó el límite de intentos.");
                        response.put("status", "AA08");
                        response.put("errors", "Usuario bloqueado por demasiados intentos fallidos.");
                        return response;

                    }else{
                        String accesoDipTermi = localIP();
                        String accesoMacTermi = dirrecionMac();
                        Libs fechaHoraService = new Libs(entityManager);
                        String accesoFecAcces = fechaHoraService.obtenerFechaYHora();
                        String accesoCodAcces = generarNumberoSerial(100000, 999999);
                        String accesoDesUsuar = cliacUsuVirtu;
                        String accesoPasUsuar = clienWwwPswrd;
                        String accesoCodTacce = "1";
                        System.out.println(accesoCodAcces);
                        String sqlInsertAccesos =
                                "INSERT INTO andacceso VALUES (:acceso_cod_acces, :acceso_des_usuar, :acceso_pas_usuar, :acceso_fec_acces, :acceso_dip_termi, :acceso_mac_termi, :acceso_cod_tacce)";
                        Query resultInsertAcceso = entityManager.createNativeQuery(sqlInsertAccesos);
                        resultInsertAcceso.setParameter("acceso_cod_acces", accesoCodAcces);
                        resultInsertAcceso.setParameter("acceso_des_usuar", accesoDesUsuar);
                        resultInsertAcceso.setParameter("acceso_pas_usuar", accesoPasUsuar);
                        resultInsertAcceso.setParameter("acceso_fec_acces", accesoFecAcces);
                        resultInsertAcceso.setParameter("acceso_dip_termi", accesoDipTermi);
                        resultInsertAcceso.setParameter("acceso_mac_termi", accesoMacTermi);
                        resultInsertAcceso.setParameter("acceso_cod_tacce", accesoCodTacce);
                        resultInsertAcceso.executeUpdate();
                    }
                }
            }
        }
        response.put("success", false);
        response.put("message", "Credenciales incorrectas.");
        response.put("status", "AA12");
        response.put("errors", "Contraseña incorrecta o usuario no encontrado.");
        return response;
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Error interno en el servidor.");
        response.put("status", "AA99");
        response.put("errors", e.getMessage());
        return response;
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
    public static String dirrecionMac() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(inetAddress);
            byte[] mac = network.getHardwareAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X:", mac[i]));
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "No disponible";
        }
    }
    public static String generarNumberoSerial(int min, int max) {
        Random random = new Random();
        int randomNumber = random.nextInt((max - min) + 1) + min;
        return String.valueOf(randomNumber);
    }
    public String codigoAleatorioTemp() {
        // Genera un número aleatorio de 6 dígitos
        Random random = new Random();
        int numeroAleatorio = 100000 + random.nextInt(900000); // Asegura 6 dígitos
        return String.valueOf(numeroAleatorio);
    }
}

