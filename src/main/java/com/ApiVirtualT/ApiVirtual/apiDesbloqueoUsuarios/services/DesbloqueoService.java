package com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.services;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.JWT.JwtUtil;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.DTO.DesbloqueoUser;
import envioCorreo.sendEmail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import libs.PassSecure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sms.SendSMS;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.*;
@Transactional
@Service
@RequiredArgsConstructor
public class DesbloqueoService {
    @PersistenceContext
    private EntityManager entityManager;


    private int intentosRealizadoTokenFallos = 0;
    public ResponseEntity<Map<String, Object>> desbloquearUsuario (DesbloqueoUser credencialesDesbloqueo){
        Map<String, Object> allData = new HashMap<>();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> allDataList = new ArrayList<>();
        HttpStatus status = HttpStatus.OK;

        String mensajeValBlancos = validarCamposBlanco(credencialesDesbloqueo);

        if(mensajeValBlancos != null){
            allData.put("message", mensajeValBlancos);
            allData.put("status", "DU01");
            allData.put("errors", "No se puede enviar campos con espacios en blanco. ");
            allDataList.add(allData);
            response.put("AllData", allDataList);

            return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> verificarExistenciaUsario = verficaUsuario(credencialesDesbloqueo.getCliacUsuVirtu(), credencialesDesbloqueo.getClienIdeClien(), credencialesDesbloqueo.getClienCodClien(), credencialesDesbloqueo.getFechaNacimiento(), credencialesDesbloqueo.getTipoIdentificacion());
        if (Boolean.TRUE.equals(verificarExistenciaUsario.get("success"))) {
            allData.put("message", "´Pasa a ingresar codigo temporal 4 digitos.");
            allData.put("status", "DU00");
            String token = (String) verificarExistenciaUsario.get("token");
            allData.put("token", token);
        }else{
            allData.put("message", verificarExistenciaUsario.get("message"));
            allData.put("status", verificarExistenciaUsario.get("status"));
            allData.put("errors", verificarExistenciaUsario.get("errors"));

        }
        allDataList.add(allData);
        response.put("AllData", allDataList);
        return new ResponseEntity<>(response, status);
    }
    public ResponseEntity<Map<String, Object>> validarCodSeguridad(HttpServletRequest request, CodSegurdiad codSeguridad) {
        try {
            String cliacUsuVirtu = (String) request.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) request.getAttribute("ClienIdenti");
            String numSocio = (String) request.getAttribute("numSocio");

            Map<String, Object> allData = new HashMap<>();
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> allDataList = new ArrayList<>();
            HttpStatus status = HttpStatus.OK;

            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                allData.put("message", "Datos del token incompletos");
                allData.put("status", "DU04");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String mensajeValidarCodigoSeguridad = validarCodigoSeguridad(codSeguridad);
            if (mensajeValidarCodigoSeguridad != null) {
                allData.put("message", mensajeValidarCodigoSeguridad);
                allData.put("status", "DU05");
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
                        String clienCedula = row2[4].toString().trim();
                        String clienCodClie = row2[5].toString().trim();
                        System.out.println("Consulta BDD= APELLIDOS: " + clienApellidos + " NOMBRES: " + clienNombres + " EMAIL: " + clienEmail + " CELULAR " + clienNumero);
                        String sqlDesblocUser = "UPDATE cnxcliac SET cliac_ctr_bloq=:bloqueo, cliac_ctr_condi=:cliac_ctr_condi  WHERE cliac_ide_clien=:cliac_ide_clien  AND cliac_usu_virtu =:cliac_usu_virtu";
                        Query resultBloqUser = entityManager.createNativeQuery(sqlDesblocUser);
                        resultBloqUser.setParameter("bloqueo", "1");
                        resultBloqUser.setParameter("cliac_ctr_condi", "1");
                        resultBloqUser.setParameter("cliac_ide_clien",clienCedula);
                        resultBloqUser.setParameter("cliac_usu_virtu", cliacUsuVirtu);
                        resultBloqUser.executeUpdate();

                        String fechaHora  = obtenerHoraActual();
                        String claveLogin = codigoAleatorioTemp();
                        PassSecure encriptarClave = new PassSecure();
                        String claveEncriptadaLogin4 = encriptarClave.encryptPassword(claveLogin);
                        System.out.println(claveEncriptadaLogin4);
                        sendEmail enviarCorreoClaveTemLogin = new sendEmail();
                        enviarCorreoClaveTemLogin.sendEmailTokenTemp(clienApellidos, clienNombres, fechaHora,clienEmail, claveLogin);
                        SendSMS enviarClave4Login = new SendSMS();
                        String FechaGenCodigo = obtenerFechaActual();
                        String HoraGenCodigo = obtenerHoraActualHora();
                        String mensajeDesbloqueo = "Estimados socio(a), el codigo de seguridad para desbloquear el usuario es: " + claveLogin + " Tiempo duracion 4 minutos. COAC ANDINA: " + FechaGenCodigo + " a las " + HoraGenCodigo;
                        enviarClave4Login.sendSMS(clienNumero,claveLogin, mensajeDesbloqueo);

                        String sqlActualContrase = "UPDATE cnxclien SET clien_www_pswrd = :clien_www_pswrd WHERE clien_ide_clien = :clien_ide_clien";
                        Query resultActuaContra = entityManager.createNativeQuery(sqlActualContrase);
                        resultActuaContra.setParameter("clien_www_pswrd", claveEncriptadaLogin4);
                        resultActuaContra.setParameter("clien_ide_clien", clienCedula);
                        resultActuaContra.executeUpdate();

                    }
                    intentosRealizadoTokenFallos = 0;
                }else{
                    intentosRealizadoTokenFallos++;
                    if (intentosRealizadoTokenFallos >= 3) {
                        String sqlBloqUser = "UPDATE cnxcliac SET cliac_ctr_bloq = :bloqueo WHERE cliac_usu_virtu = :username";
                        Query resultBloqUser = entityManager.createNativeQuery(sqlBloqUser);
                        resultBloqUser.setParameter("bloqueo", "0");
                        resultBloqUser.setParameter("username", cliacUsuVirtu);
                        //MANDAR CORREO DE BLOQUEO
                        try {
                            int rowsUpdated = resultBloqUser.executeUpdate();
                            if (rowsUpdated > 0) {
                                intentosRealizadoTokenFallos = 0;
                                response.put("success", false);
                                response.put("message", "Usuario bloqueado por exceder límite de intentos");
                                response.put("status", "DU06");
                            }
                        } catch (Exception e) {
                            response.put("success", false);
                            response.put("message", "Error al intentar bloquear el usuario");
                            response.put("status", "DU07");
                        }
                    } else {
                        response.put("success", false);
                        response.put("message", "Token incorrecto. Intentos restantes: " + (4 - intentosRealizadoTokenFallos));
                        response.put("status", "DU08");
                    }
                }
            }else {
                allData.put("status", "DU09");
                allData.put("errors", "TOKEN INCORRECTO");
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



    public String validarCamposBlanco(DesbloqueoUser credencialesDesbloqueo){
        if(credencialesDesbloqueo.getCliacUsuVirtu() == null || credencialesDesbloqueo.getCliacUsuVirtu().isEmpty()){
            return "El usuario no puede estar en blanco";

        }
        if(credencialesDesbloqueo.getClienIdeClien() == null || credencialesDesbloqueo.getClienIdeClien().isEmpty()){
            return "El numero de indentificacion no puede estar en blanco";

        }
        if(credencialesDesbloqueo.getClienCodClien() == null || credencialesDesbloqueo.getClienCodClien().isEmpty()){
            return  "El numero de socio no puede estar en blanco";
        }
        if(credencialesDesbloqueo.getFechaNacimiento() == null || credencialesDesbloqueo.getFechaNacimiento().isEmpty()
        ){
            return "La fecha de nacimiento no puede estar en blanco";
        }

        return null;
    }


    public Map<String,Object> verficaUsuario(String usuario, String identificacionUser, String codigoUsuario,
                                         String fechaNacUsuario, String tipoIdentificacion) {

        Map<String, Object> response = new HashMap<>();


        List<Object[]> verificarExistenciaUsario = new ArrayList<>();
        try {
            String sqlVerificarUserDesbloq =
                    "SELECT cliac_ide_clien, cliac_usu_virtu, clien_cod_tiden, clien_cod_clien, clien_ape_clien, clien_nom_clien, clien_dir_email, clien_tlf_celul, clien_www_pswrd, clien_fec_nacim " +
                            "FROM cnxcliac, cnxclien " +
                            "WHERE cliac_usu_virtu = :cliac_usu_virtu " +
                            "AND cliac_ide_clien = :cliac_ide_clien " +
                            "AND clien_cod_clien = :clien_cod_clien " +
                            "AND clien_cod_tiden = :clien_cod_tiden " +
                            "AND cliac_ide_clien = clien_ide_clien";

            Query queryVerfUsuario = entityManager.createNativeQuery(sqlVerificarUserDesbloq);
            queryVerfUsuario.setParameter("cliac_usu_virtu", usuario);
            queryVerfUsuario.setParameter("clien_cod_clien", codigoUsuario);
            queryVerfUsuario.setParameter("cliac_ide_clien", identificacionUser);
            queryVerfUsuario.setParameter("clien_cod_tiden", tipoIdentificacion);


            verificarExistenciaUsario = queryVerfUsuario.getResultList();

            if (!verificarExistenciaUsario.isEmpty()) {
                for (Object[] row0 : verificarExistenciaUsario) {
                    String cliacIdeClien = row0[0].toString().trim();
                    String cliacUsuVirtual = row0[1].toString().trim();
                    String clieCodTien = row0[2].toString().trim();
                    String clienCodigoClien = row0[3].toString().trim();
                    String clienApellidoClien = row0[4].toString().trim();
                    String clieNomClien = row0[5].toString().trim();
                    String clieDirEmailCli = row0[6].toString().trim();
                    String clieNumCelular = row0[7].toString().trim();
                    String clienPassword = row0[8].toString().trim();
                    String fechaNaciClien = row0[9].toString().trim();
                    DateTimeFormatter formatoEntrada = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    DateTimeFormatter formatoSalida = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    String fechaNaciFormateada = LocalDate.parse(fechaNaciClien, formatoEntrada).format(formatoSalida);
                    System.out.println("Fecha de nacimiento formateada: " + fechaNaciFormateada);

                    if(cliacUsuVirtual.equals(usuario) && cliacIdeClien.equals(identificacionUser) && clieCodTien.equals(tipoIdentificacion)
                            && clienCodigoClien.equals(codigoUsuario) && fechaNaciFormateada.equals(fechaNacUsuario)){
                        System.out.println("Los usuarios si coinciden");
                        String CodigoDesbloqueo = codigoAleatorioTemp();
                        String FechaGenCodigo = obtenerFechaActual();
                        String HoraGenCodigo = obtenerHoraActualHora();
                        String FechaDesbloqueoUser = obtenerHoraActual();
                        String mensajeDesbloqueo = "Estimados socio(a), el codigo de seguridad para desbloquear el usuario es: " + CodigoDesbloqueo + " Tiempo duracion 4 minutos. COAC ANDINA: " + FechaGenCodigo + " a las " + HoraGenCodigo;
                        SendSMS smsDesbloqueo = new SendSMS();
                        smsDesbloqueo.sendSMS(clieNumCelular, "1150", mensajeDesbloqueo);
                        sendEmail enviarCorreo = new sendEmail();
                        enviarCorreo.sendEmailTokenTemp(clienApellidoClien, clieNomClien, FechaDesbloqueoUser, clieDirEmailCli, CodigoDesbloqueo);
                        String sqlUpdateEstado = "UPDATE vircodaccess SET codaccess_estado = '0' WHERE codaccess_cedula = :codaccess_cedula AND codaccess_estado = '1'";
                        Query resultUpdateEstado = entityManager.createNativeQuery(sqlUpdateEstado);
                        resultUpdateEstado.setParameter("codaccess_cedula", cliacIdeClien);
                        resultUpdateEstado.executeUpdate();

                        String sqlInsertToken = "INSERT INTO vircodaccess (codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal, codsms_codigo, codaccess_estado, codaccess_fecha) VALUES (:codaccess_cedula, :codaccess_usuario, :codaccess_codigo_temporal, :codsms_codigo, :codaccess_estado, :codaccess_fecha)";

                        Query resultInsertTokenAcceso = entityManager.createNativeQuery(sqlInsertToken);

                        resultInsertTokenAcceso.setParameter("codaccess_cedula", cliacIdeClien);
                        resultInsertTokenAcceso.setParameter("codaccess_usuario", cliacUsuVirtual);
                        resultInsertTokenAcceso.setParameter("codaccess_codigo_temporal", CodigoDesbloqueo);
                        resultInsertTokenAcceso.setParameter("codsms_codigo", 4);
                        resultInsertTokenAcceso.setParameter("codaccess_estado", "1");
                        resultInsertTokenAcceso.setParameter("codaccess_fecha", FechaDesbloqueoUser);

                        resultInsertTokenAcceso.executeUpdate();

                        String token = JwtUtil.generateToken(cliacUsuVirtual, cliacIdeClien, clienCodigoClien);
                        response.put("success", true);
                        response.put("message", "Acceso concedido.");
                        response.put("status", "AA00");
                        response.put("token", token);
                        return response;
                    }
                }
                response.put("success", false);
                response.put("message", "Error: los datos ingresados no coinciden con la base de datos.");
                response.put("status", "DU03");
                response.put("errors", "Las credenciales ingresadas no coinciden con la información registrada.");
            } else {
                response.put("success", false);
                response.put("message", "Usuario no registrado con los datos ingresados, por favor verifique e intente de nuevo.");
                response.put("status", "DU02");
                response.put("errors", "No se encontró información del usuario.");
            }

        } catch (Exception e) {
            response.put("message", "Ocurrió un error inesperado al verificar el usuario.");
            response.put("error", e.getMessage());
            response.put("status", "ERROR");
        }
        return response;
    }
    public String validarCodigoSeguridad(CodSegurdiad request) {

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
    public static String obtenerHoraActualHora() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    public static String obtenerFechaActual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    public static String obtenerHoraActual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


}
