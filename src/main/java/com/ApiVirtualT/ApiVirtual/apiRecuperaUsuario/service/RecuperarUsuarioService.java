package com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.service;

import com.ApiVirtualT.ApiVirtual.apiAutenticacion.JWT.JwtUtil;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.dto.CodSegRequest;
import com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.dto.RecuperarUserRequest;
import com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.DTO.DatosRegistro;
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
import passwords.passwordSecureToken;
import sms.SendSMS;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.TokenExpirationService;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Transactional
@Service
@RequiredArgsConstructor
public class RecuperarUsuarioService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TokenExpirationService tokenExpirationService;
    private int intentosRealizados = 0, intentosRealizadoTokenFallos = 0;

    public ResponseEntity<Map<String, Object>> recuperarUsuarios(RecuperarUserRequest request){
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> allData = new HashMap<>();
        List<Map<String, Object>> allDataList = new ArrayList<>();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        try {
            // Validación del identificador (cédula/RUC/pasaporte)
            if (request.getNumIdentifcacion() == null || request.getNumIdentifcacion().trim().isEmpty()) {
                allData.put("message", "El número de identificación no puede estar vacío.");
                allData.put("status", "AA01");
                allData.put("errors", "Campo de identificación obligatorio.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validación de formato de identificación
            String identificacion = request.getNumIdentifcacion().trim();
            // Regex para validar: cédula (10 dígitos), RUC (13 dígitos), pasaporte (alfanumérico de 5-13 caracteres)
            if (!Pattern.matches("^([0-9]{10}|[0-9]{13}|[A-Za-z0-9]{5,13})$", identificacion)) {
                allData.put("message", "Formato de identificación inválido.");
                allData.put("status", "AA02");
                allData.put("errors", "El formato debe corresponder a cédula, RUC o pasaporte ecuatoriano.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validación de fecha de nacimiento
            if (request.getFechaNacimiento() == null || request.getFechaNacimiento().trim().isEmpty()) {
                allData.put("message", "La fecha de nacimiento no puede estar vacía.");
                allData.put("status", "AA03");
                allData.put("errors", "Campo de fecha de nacimiento obligatorio.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validación del formato de fecha
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate fechaNacimiento = LocalDate.parse(request.getFechaNacimiento().trim(), formatter);
                LocalDate fechaActual = LocalDate.now();

                if (fechaNacimiento.isAfter(fechaActual)) {
                    allData.put("message", "La fecha de nacimiento no puede ser futura.");
                    allData.put("status", "AA04");
                    allData.put("errors", "Ingrese una fecha de nacimiento válida.");
                    allDataList.add(allData);
                    response.put("AllData", allDataList);
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            } catch (DateTimeParseException e) {
                allData.put("message", "Formato de fecha inválido.");
                allData.put("status", "AA05");
                allData.put("errors", "El formato debe ser dd/MM/yyyy.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }


            String identificacionClien = request.getNumIdentifcacion().trim();
            String fechaNacClien = request.getFechaNacimiento().trim();
            Integer ctrControlAccion = request.getCtrEstado();
            String dirEmail = request.getDirEmail().trim();


            if(ctrControlAccion == 1){

                String sqlValidarDatos = "SELECT clien_ide_clien, clien_tlf_celul, clien_fec_nacim, clien_dir_email FROM cnxclien " +
                        "WHERE clien_ide_clien = :clien_ide_clien  AND clien_fec_nacim = TO_DATE(:clien_fec_nacim, '%d/%m/%Y')";
                Query resulValidarCampos = entityManager.createNativeQuery(sqlValidarDatos);
                resulValidarCampos.setParameter("clien_ide_clien", identificacionClien);
                resulValidarCampos.setParameter("clien_fec_nacim", fechaNacClien);
                List<?> resultados = resulValidarCampos.getResultList();

                if(!resultados.isEmpty()){

                    String numCelular = "";
                    String dirCorreo = "";
                    for (Object resultado : resultados) {
                        Object[] row = (Object[]) resultado;
                        numCelular = row[1].toString().trim();
                        dirCorreo = row[3].toString().trim();

                    }
                    response.put("message", "VERIFICACION DE INFORMACION CORRECTA PASA A VALIDAR CORREO");
                    response.put("email",dirCorreo);
                    response.put("status", "OKB0254");
                    return new ResponseEntity<>(response, HttpStatus.OK );

                }else{
                    response.put("message", "LA INFORMACION INGRESADA NO ES LA CORRECTA, INTENTE NUEVAMENTE ");
                    response.put("status", "ERR014523");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

            }else{

                //VALIDAR QUE EL CORREO Y LA CONTRASEÑA ESTA BIEN PARA VALIDAR

                String CodigoDesbloqueo = codigoAleatorioTemp();
                Libs fechaHoraService = new Libs(entityManager);
                String FechaDesbloqueoUser = fechaHoraService.obtenerFechaYHora();
                String fechaHora = fechaHoraService.obtenerFechaYHora();
                String sql = """
                    SELECT clien_dir_email,clien_tlf_celul  FROM cnxclien WHERE clien_ide_clien = :clien_ide_clien;
                    """;
                Query query = entityManager.createNativeQuery(sql);
                query.setParameter("clien_ide_clien", identificacionClien);

                @SuppressWarnings("unchecked")
                List<Object[]> resultados = query.getResultList();

                if (resultados.isEmpty()) {
                    response.put("message", "No se encuentra un usuario registrado con la identificacion ingresada.");
                    response.put("status", "ERROR002");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }else{
                    String txtEmailBase = "";
                    String txtNumCelular = "";
                    for (Object[] row : resultados) {
                        txtEmailBase = row[0].toString().trim();
                        txtNumCelular= row[1].toString().trim();
                    }
                    System.err.println(txtEmailBase);
                    System.err.println(txtNumCelular);
                    System.err.println(dirEmail);

                    if(dirEmail.trim().equals(txtEmailBase)){

                        String sqlValidarDatos = "SELECT clien_ide_clien, clien_tlf_celul, clien_fec_nacim, clien_dir_email,cliac_usu_virtu, clien_cod_clien" +
                                " FROM cnxclien, cnxcliac " +
                                "WHERE clien_ide_clien = :clien_ide_clien  AND clien_fec_nacim = TO_DATE(:clien_fec_nacim, '%d/%m/%Y') " +
                                "AND clien_ide_clien = cliac_ide_clien";
                        Query resulValidarCampos = entityManager.createNativeQuery(sqlValidarDatos);
                        resulValidarCampos.setParameter("clien_ide_clien", identificacionClien);
                        resulValidarCampos.setParameter("clien_fec_nacim", fechaNacClien);
                        List<?> resultados1 = resulValidarCampos.getResultList();

                        if(!resultados1.isEmpty()) {

                            String numCelular = "";
                            String dirCorreo = "";
                            String txtUsuar = "";
                            String codUsuar = "";
                            for (Object resultado : resultados1) {
                                Object[] row = (Object[]) resultado;
                                numCelular = row[1].toString().trim();
                                dirCorreo = row[3].toString().trim();
                                txtUsuar = row[4].toString().trim();
                                codUsuar = row[5].toString().trim();
                            }

                                SendSMS smsDesbloqueo = new SendSMS();
                                smsDesbloqueo.sendSecurityCodeSMS(numCelular,"1150",CodigoDesbloqueo,"Recuperar su usuario",FechaDesbloqueoUser);

                                // NUEVAS CONSULTAS PARA ACTUALIZAR CÓDIGOS
                                // Actualizar estado de códigos anteriores a 0
                                String sqlUpdateEstadoAnteriores = "UPDATE vircodaccess SET codaccess_estado = '0' " +
                                        "WHERE codaccess_cedula = :codaccess_cedula  AND codaccess_estado = '1' AND codsms_codigo = 7";
                                Query queryUpdateEstadoAnteriores = entityManager.createNativeQuery(sqlUpdateEstadoAnteriores);
                                queryUpdateEstadoAnteriores.setParameter("codaccess_cedula", identificacionClien);
                                queryUpdateEstadoAnteriores.executeUpdate();

                                // Insertar nuevo código temporal
                                String sqlInsertNuevoCodigo = "INSERT INTO vircodaccess " +
                                        "(codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal, codaccess_estado, codsms_codigo, codaccess_fecha) " +
                                        "VALUES (:codaccess_cedula, :codaccess_usuario, :codaccess_codigo_temporal, '1', 7, :codaccess_fecha)";
                                Query queryInsertNuevoCodigo = entityManager.createNativeQuery(sqlInsertNuevoCodigo);
                                queryInsertNuevoCodigo.setParameter("codaccess_cedula", identificacionClien);
                                queryInsertNuevoCodigo.setParameter("codaccess_usuario", txtUsuar);
                                queryInsertNuevoCodigo.setParameter("codaccess_codigo_temporal", CodigoDesbloqueo);
                                queryInsertNuevoCodigo.setParameter("codaccess_fecha", fechaHora);
                                queryInsertNuevoCodigo.executeUpdate();
                                tokenExpirationService.programarExpiracionToken(identificacionClien, CodigoDesbloqueo, "7");

                                String token = JwtUtil.generateToken(txtUsuar, identificacionClien, codUsuar);

                                response.put("email", txtEmailBase);
                                response.put("numeroCelular", txtNumCelular);
                                response.put("message","CORREO COINCIDE CON BDD");
                                response.put("token",token);
                                response.put("status","OKBDD005");

                        }else{
                            response.put("message","No se puede validar coincidencias de correo electronico, no existe informacion ingresada.");
                            response.put("status","ERR02541");
                            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                        }
                    }else{
                        response.put("message","El correo ingresado no coincide con nuestros registros.");
                        response.put("status","ERR02541");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }
                }
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
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

    public ResponseEntity<Map<String, Object>> validarCodSeguridad(HttpServletRequest request, CodSegRequest codSeguridad) {
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
            if (codSeguridad.getCodaccess_codigo_temporal() == null || !codSeguridad.getCodaccess_codigo_temporal().trim().matches("\\d{4}")) {
                allData.put("message", "Código de seguridad inválido");
                allData.put("status", "AA023");
                allData.put("errors", "El código debe contener exactamente 4 dígitos");
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

            String sqlVerificaTokenBDD = "SELECT FIRST 1  codaccess_codigo_temporal FROM vircodaccess " +
                    "WHERE codaccess_cedula = :codaccess_cedula AND codaccess_usuario = :codaccess_usuario AND codaccess_estado = :codaccess_estado ORDER BY codaccess_id DESC ";
            Query queryVerificaTokenBDD = entityManager.createNativeQuery(sqlVerificaTokenBDD);
            queryVerificaTokenBDD.setParameter("codaccess_cedula", clienIdenti);
            queryVerificaTokenBDD.setParameter("codaccess_usuario", cliacUsuVirtu);
            queryVerificaTokenBDD.setParameter("codaccess_estado", "1");
            List<Object[]> resultsTokenBDD = queryVerificaTokenBDD.getResultList();
            if (!resultsTokenBDD.isEmpty()) {
                String tokenFromDB = (String) queryVerificaTokenBDD.getSingleResult();
                if (tokenFromDB != null && codSeguridad.getCodaccess_codigo_temporal() != null &&
                        codSeguridad.getCodaccess_codigo_temporal().trim().equals(tokenFromDB.trim())) {
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
                        String IpIngresoLogin = localIP();

                        Libs fechaHoraService = new Libs(entityManager);
                        String FechaIngresoLogin = fechaHoraService.obtenerFechaYHora();
                        System.out.println(FechaIngresoLogin);

                        sendEmail enviarCorreo = new sendEmail();
                        enviarCorreo.sendRecupeUsuario(clienApellidos,clienNombres, FechaIngresoLogin, clienEmail, cliacUsuVirtu);

                        allData.put("status", "AUTHO");
                        allData.put("message", "SE HA ENVIADO CORRECTAMENTE EL USUARIO RECUPERADO AL CORREO");
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

                                    String accesoDipTermi = localIP();
                                    String accesoMacTermi = dirrecionMac();
                                    Libs fechaHoraService2 = new Libs(entityManager);
                                    String accesoFecAcces = fechaHoraService2.obtenerFechaYHora();
                                    String accesoCodAcces = generarNumberoSerial(1000000, 99999999);
                                    String accesoDesUsuar = cliacUsuVirtu;
                                    String accesoCodTacce = "2";
                                    System.out.println(accesoCodAcces);
                                    String sqlInsertAccesos =
                                            "INSERT INTO andacceso VALUES (:acceso_cod_acces, :acceso_des_usuar, :acceso_pas_usuar, :acceso_fec_acces, :acceso_dip_termi, :acceso_mac_termi, :acceso_cod_tacce)";
                                    Query resultInsertAcceso = entityManager.createNativeQuery(sqlInsertAccesos);
                                    resultInsertAcceso.setParameter("acceso_cod_acces", accesoCodAcces);
                                    resultInsertAcceso.setParameter("acceso_des_usuar", accesoDesUsuar);
                                    resultInsertAcceso.setParameter("acceso_pas_usuar", "");
                                    resultInsertAcceso.setParameter("acceso_fec_acces", accesoFecAcces);
                                    resultInsertAcceso.setParameter("acceso_dip_termi", accesoDipTermi);
                                    resultInsertAcceso.setParameter("acceso_mac_termi", accesoMacTermi);
                                    resultInsertAcceso.setParameter("acceso_cod_tacce", accesoCodTacce);
                                    resultInsertAcceso.executeUpdate();

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
                        response.put("message", "Código temporal incorrecto. Intentos restantes: " + (3 - intentosRealizadoTokenFallos));
                        response.put("status", "AA058");
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
    public static String generarNumberoSerial(int min, int max) {
        Random random = new Random();
        int randomNumber = random.nextInt((max - min) + 1) + min;
        return String.valueOf(randomNumber);
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
    public static String localIP() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "No disponible";
        }
    }

    public String validarCodigoSeguridad(CodSegRequest request) {

        if (request.getCodaccess_codigo_temporal() == null || request.getCodaccess_codigo_temporal().trim().isEmpty()) {
            return "El código temporal no puede estar vacío o contener solo espacios.";
        }

        if (request.getCodaccess_codigo_temporal().length() < 4) {
            return "El código temporal debe tener al menos 4 caracteres.";
        }

        return null;
    }





    public String codigoAleatorioTemp() {
        // Genera un número aleatorio de 4 dígitos
        Random random = new Random();
        int numeroAleatorio = 1000 + random.nextInt(9000); // Asegura 4 dígitos
        return String.valueOf(numeroAleatorio);
    }
}
