package com.ApiVirtualT.ApiVirtual.apiAutenticacion.services;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.UserCredentials;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CambioContrasenaCredencial;
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

    /**
     * Funcion para validar login antes de consultar bdd
        */

    private int intentosRealizados = 0;
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

        String mensajeValCaracEspeUser = validarUsuarioCaracEspeciales(request);
        if (mensajeValCaracEspeUser != null) {
            allData.put("message", mensajeValCaracEspeUser);
            allData.put("status", "AA03");
            allData.put("errors", "No se aceptan caracteres especiales en el usuario. ");
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
                allData.put("token", generarTokenSeguro());


            } else {

                allData.put("message", validacion.get("message"));
                allData.put("status", validacion.get("status"));
                allData.put("errors", validacion.get("errors"));
            }
            allDataList.add(allData);
            response.put("AllData", allDataList);
        }else{

            allData.put("message", "Por favor, no utilice la identificación como si fuera un nombre de usuario.");
            allData.put("status", "AA04");
            allData.put("errors", "Se debe utilizar el nombre de usuario en lugar de la cédula de identificación.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
        }

        return new ResponseEntity<>(response, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    public String validarUsuarioCaracEspeciales(UserCredentials request) {
        String usuario = request.getCliacUsuVirtu();
        String regexCorreo = "^[\\w-]+(?:\\.[\\w-]+)*@[\\w-]+(?:\\.[\\w-]+)+$";

        String regexCaracteresEspeciales = ".*[^a-zA-Z0-9-_].*";
        if (usuario.matches(regexCorreo)) {
            return "El nombre de usuario no puede ser un correo electrónico.";
        }

        if (usuario.matches(regexCaracteresEspeciales)) {
            return "El nombre de usuario no puede contener caracteres especiales.";
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

            if(passDec.length() <= 4){
                response.put("success", false);
                response.put("message", "Debe cambiar su contraseña o es contraseña temporal");
                response.put("status", "AA06");
                response.put("errors", "Usuario con contraseña temporal.");
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
                            String sqlDatosCorreo = "SELECT clien_ape_clien, clien_nom_clien, clien_dir_email, clien_tlf_celul FROM cnxclien, cnxcliac " +
                                    "WHERE cliac_usu_virtu = :username AND clien_ide_clien = cliac_ide_clien";
                            Query resulDatosCorreo = entityManager.createNativeQuery(sqlDatosCorreo);
                            resulDatosCorreo.setParameter("username", user);

                            List<Object[]> results2 = resulDatosCorreo.getResultList();
                            for (Object[] row2 : results2) {
                                String clienApellidos =  row2[0].toString().trim();
                                String clienNombres =  row2[1].toString().trim();
                                String clienEmail = row2[2].toString().trim();
                                String clienNumero = row2[3].toString().trim();
                                System.out.println("Consulta BDD= APELLIDOS: " + clienApellidos + " NOMBRES: " + clienNombres + " EMAIL: " + clienEmail + " CELULAR "+ clienNumero);
                                String IpIngresoLogin = localIP();
                                String FechaIngresoLogin = obtenerHoraActual();

                                SendSMS sms = new SendSMS();
                                String mensajeSMSLogin = "Registro de Acceso a Banca Movil. Att, Cooperativa Andina. "+FechaIngresoLogin + "";
                                sms.sendSMS(clienNumero,"1050", mensajeSMSLogin);
                            sendEmail enviarCorreo =  new sendEmail();
                            enviarCorreo.sendEmailInicioSesion(clienApellidos, clienNombres, FechaIngresoLogin, IpIngresoLogin, clienEmail );

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
                        String accesoFecAcces = obtenerHoraActual();
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

    /**
     * Metodo para realizar el cambio de contraseña.
     */

    public ResponseEntity<Map<String, Object>> cambiarContrasena(CambioContrasenaCredencial request) {
        Map<String, Object> allData = new HashMap<>();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> allDataList = new ArrayList<>();

        // Validar coincidencia de la nueva contraseña y su confirmación
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            allData.put("message", "La nueva contraseña y su confirmación no coinciden.");
            allData.put("status", "AA09");
            allData.put("errors", "No se aceptan contraseñas diferentes en el cambio de contraseña. ");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        }
        // Validar longitud de la nueva contraseña
        if (request.getNewPassword().length() <= 4) {
            allData.put("message", "La contraseña debe tener al menos 4 caracteres. Por favor, inténtelo nuevamente.");
            allData.put("status", "AA10");
            allData.put("errors", "No se aceptan contraseña menores a 4 caractreres ");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
            if (request.getNewPassword().isEmpty()) {
                allData.put("message", "No puede dejar espacios en blanco en la nueva contraseña.");
                allData.put("status", "AA11");
                allData.put("errors", "No se aceptan espacios en blanco en la contraseña. ");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

            }
            if (request.getConfirmPassword().isEmpty()) {
                allData.put("message", "No puede dejar espacios en blanco en la confirmacion de la contraseña.");
                allData.put("status", "AA12");
                allData.put("errors", "No se aceptan espacios en blanco en la contraseña. ");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

            }

            // Retornar éxito si el cambio fue realizado
            allData.put("message", "Contraseña actualizada exitosamente.");
            allData.put("status", "AA13");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.OK);

        }

        // Aquí implementarías el almacenamiento de la nueva contraseña en la base de datos

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

    public static String obtenerHoraActual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    public static String generarNumberoSerial(int min, int max) {
        Random random = new Random();
        int randomNumber = random.nextInt((max - min) + 1) + min;
        return String.valueOf(randomNumber);
    }
    public static String generarTokenSeguro() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[32]; // 256 bits = 32 bytes
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    }
