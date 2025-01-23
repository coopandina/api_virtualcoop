package com.ApiVirtualT.ApiVirtual.apiCambioPassword.services;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.JWT.JwtUtil;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.TokenExpirationService;
import com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO.CambioContrasena;
import com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO.CambioPassUser;
import com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO.ValidacionDatos;
import com.ApiVirtualT.ApiVirtual.libs.Libs;
import envioCorreo.sendEmail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import libs.PassSecure;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sms.SendSMS;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Transactional
@Service
@RequiredArgsConstructor
public class CambioPassService {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TokenExpirationService tokenExpirationService;

    private int intentosRealizadoTokenFallos = 0;
    public ResponseEntity<Map<String, Object>> cambioPassUsuario (CambioPassUser credencialPassUser){
        Map<String, Object> allData = new HashMap<>();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> allDataList = new ArrayList<>();
        HttpStatus status = HttpStatus.OK;

        String mensajeValBlancos = validarCamposBlanco(credencialPassUser);

        if(mensajeValBlancos != null){
            allData.put("message", mensajeValBlancos);
            allData.put("status", "DU01");
            allData.put("errors", "No se puede enviar campos con espacios en blanco. ");
            allDataList.add(allData);
            response.put("AllData", allDataList);

            return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
        }
        if (credencialPassUser.getFechaNacimiento() == null || !credencialPassUser.getFechaNacimiento().matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/([12][0-9]{3})$")) {
            allData.put("message", "Formato de fecha inválido");
            allData.put("status", "DU22");
            allData.put("errors", "La fecha debe tener el formato DD/MM/YYYY");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (credencialPassUser.getClienCodClien() == null || !credencialPassUser.getClienCodClien().matches("^[0-9]+$")) {
            allData.put("message", "Código de cliente inválido");
            allData.put("status", "DU23");
            allData.put("errors", "El código de cliente debe contener solo números");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (credencialPassUser.getTipoIdentificacion() == null || !credencialPassUser.getTipoIdentificacion().matches("^[0-9]+$")) {
            allData.put("message", "Tipo de identificación inválido");
            allData.put("status", "DU24");
            allData.put("errors", "El tipo de identificación debe contener solo números");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        String repsuestaValidarCedula = validarCedulaEC(credencialPassUser);
        if(repsuestaValidarCedula != null){
            allData.put("message", repsuestaValidarCedula);
            allData.put("status", "AA035");
            allData.put("errors", "La cedula de ciudadania ingresada no es correcta o no existe");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> verificarExistenciaUsario = verficaUsuario(credencialPassUser.getCliacUsuVirtu(), credencialPassUser.getClienIdeClien(), credencialPassUser.getClienCodClien(), credencialPassUser.getFechaNacimiento(), credencialPassUser.getTipoIdentificacion());
        if (Boolean.TRUE.equals(verificarExistenciaUsario.get("success"))) {
            allData.put("message", "Pasa a ingresar codigo temporal 4 digitos.");
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
                        Libs fechaHoraService = new Libs(entityManager);
                        String FechaDesbloqueoUser = fechaHoraService.obtenerFechaYHora();


                        SendSMS smsDesbloqueo = new SendSMS();
                        smsDesbloqueo.sendSecurityCodeSMS(clieNumCelular,"1150",CodigoDesbloqueo,"Actualizar su Clave",FechaDesbloqueoUser);
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
                        resultInsertTokenAcceso.setParameter("codsms_codigo", 5);
                        resultInsertTokenAcceso.setParameter("codaccess_estado", "1");
                        resultInsertTokenAcceso.setParameter("codaccess_fecha", FechaDesbloqueoUser);
                        resultInsertTokenAcceso.executeUpdate();
                        tokenExpirationService.programarExpiracionToken(cliacIdeClien, CodigoDesbloqueo, "5");

                        String token = JwtUtil.generateToken(cliacUsuVirtual, cliacIdeClien, clienCodigoClien);
                        response.put("success", true);
                        response.put("message", "Acceso concedido.");
                        response.put("status", "CC00");
                        response.put("token", token);
                        return response;
                    }
                }
                response.put("success", false);
                response.put("message", "Error: los datos ingresados no coinciden con la base de datos.");
                response.put("status", "CC02");
                response.put("errors", "Las credenciales ingresadas no coinciden con la información registrada.");
            } else {
                response.put("success", false);
                response.put("message", "Usuario no registrado con los datos ingresados, por favor verifique e intente de nuevo.");
                response.put("status", "CC01");
                response.put("errors", "No se encontró información del usuario.");
            }

        } catch (Exception e) {
            response.put("message", "Ocurrió un error inesperado al verificar el usuario.");
            response.put("error", e.getMessage());
            response.put("status", "ERROR");
        }
        return response;
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
                    "WHERE codaccess_cedula = :codaccess_cedula AND codaccess_usuario = :codaccess_usuario AND codaccess_estado = :codaccess_estado AND codsms_codigo = 5  ";
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
                        Libs fechaHoraService = new Libs(entityManager);
                        String fechaHora = fechaHoraService.obtenerFechaYHora();

                        String claveLogin = codigoAleatorioTemp();
                        PassSecure encriptarClave = new PassSecure();
                        String claveEncriptadaLogin4 = encriptarClave.encryptPassword(claveLogin);
                        System.out.println(claveEncriptadaLogin4);

                        // NUEVAS CONSULTAS PARA ACTUALIZAR CÓDIGOS
                        // Actualizar estado de códigos anteriores a 0
                        String sqlUpdateEstadoAnteriores = "UPDATE vircodaccess SET codaccess_estado = '0' " +
                                "WHERE codaccess_cedula = :codaccess_cedula AND codaccess_usuario = :codaccess_usuario AND codaccess_estado = '1' AND codsms_codigo = 5";
                        Query queryUpdateEstadoAnteriores = entityManager.createNativeQuery(sqlUpdateEstadoAnteriores);
                        queryUpdateEstadoAnteriores.setParameter("codaccess_cedula", clienCedula);
                        queryUpdateEstadoAnteriores.setParameter("codaccess_usuario", cliacUsuVirtu);
                        queryUpdateEstadoAnteriores.executeUpdate();

                        // Insertar nuevo código temporal
                        String sqlInsertNuevoCodigo = "INSERT INTO vircodaccess " +
                                "(codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal, codaccess_estado, codsms_codigo, codaccess_fecha) " +
                                "VALUES (:codaccess_cedula, :codaccess_usuario, :codaccess_codigo_temporal, '1', 5, :codaccess_fecha)";
                        Query queryInsertNuevoCodigo = entityManager.createNativeQuery(sqlInsertNuevoCodigo);
                        queryInsertNuevoCodigo.setParameter("codaccess_cedula", clienCedula);
                        queryInsertNuevoCodigo.setParameter("codaccess_usuario", cliacUsuVirtu);
                        queryInsertNuevoCodigo.setParameter("codaccess_codigo_temporal", claveLogin);
                        queryInsertNuevoCodigo.setParameter("codaccess_fecha", fechaHora);
                        queryInsertNuevoCodigo.executeUpdate();
                        tokenExpirationService.programarExpiracionToken(clienCedula, claveLogin, "5");

                        sendEmail enviarCorreoClaveTemLogin = new sendEmail();
                        enviarCorreoClaveTemLogin.sendEmailClaveTemp(clienApellidos, clienNombres, fechaHora,clienEmail, claveLogin);
                        SendSMS enviarClave4Login = new SendSMS();
                        Libs fechaHoraService1 = new Libs(entityManager);
                        String FechaGenCodigo = fechaHoraService1.obtenerFecha();

                        String HoraGenCodigo = fechaHoraService1.obtenerHora();

                        String mensajeDesbloqueo = "Estimados socio(a), el codigo de seguridad para desbloquear el usuario es: " + claveLogin + " Tiempo duracion 4 minutos. COAC ANDINA: " + FechaGenCodigo + " a las " + HoraGenCodigo;
                        enviarClave4Login.sendSMS(clienNumero,claveLogin, mensajeDesbloqueo);

                        String sqlActualContrase = "UPDATE cnxclien SET clien_www_pswrd = :clien_www_pswrd WHERE clien_ide_clien = :clien_ide_clien";
                        Query resultActuaContra = entityManager.createNativeQuery(sqlActualContrase);
                        resultActuaContra.setParameter("clien_www_pswrd", claveEncriptadaLogin4);
                        resultActuaContra.setParameter("clien_ide_clien", clienCedula);
                        resultActuaContra.executeUpdate();
                    }
                    allData.put("message", "Pasa a validar informacion del usuario ok !!");
                    allData.put("status", "DU10");
                    allDataList.add(allData);
                    response.put("AllData", allDataList);
                    intentosRealizadoTokenFallos = 0;
                    return new ResponseEntity<>(response, HttpStatus.OK);
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
                                    emailBloq.sendEmailBloqueo(clienApellidos, clienNombres, FechaHora, clienEmail, IpIngreso);
                                intentosRealizadoTokenFallos = 0;
                                response.put("success", false);
                                response.put("message", "Usuario bloqueado por exceder límite de intentos");
                                response.put("status", "DU06");
                                }
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
                allData.put("errors", "Codigo temporal expirado por exceder el tiempo limite de 4 minutos.");
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

        if (request.getCodaccess_codigo_temporal().length() < 4) {
            return "El código temporal debe tener al menos 4 caracteres.";
        }

        return null;
    }
    public ResponseEntity<Map<String, Object>> validarDatosUsuario(HttpServletRequest request, ValidacionDatos validacionDatos) {
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
            if(clienIdenti.equals(validacionDatos.getClienIdeClien())){
                String sqlDatosUsuario = "SELECT clien_ape_clien, clien_nom_clien, clien_dir_email, clien_tlf_celul, clien_fec_nacim FROM cnxclien, cnxcliac " +
                        "WHERE clien_ide_clien = :clien_ide_clien AND clien_ide_clien = cliac_ide_clien";
                Query resulDatosVerificadoUser = entityManager.createNativeQuery(sqlDatosUsuario);
                resulDatosVerificadoUser.setParameter("clien_ide_clien", clienIdenti);
                List<Object[]> result1 = resulDatosVerificadoUser.getResultList();
                for(Object[] row1 : result1){
                    String clienApellidos = row1[0].toString().trim();
                    String clienNombres = row1[1].toString().trim();
                    String clienEmail = row1[2].toString().trim();
                    String clienNumero = row1[3].toString().trim();
                    String clienFechaNac = row1[4].toString().trim();
                    String sqlConsulPgSeguri = "SELECT pgvcs_des_pgvcs FROM cnxcliac,andpgvcs WHERE (cliac_ide_clien= :cliac_ide_clien OR cliac_usu_virtu= :cliac_usu_virtu) AND cliac_pgt_1=pgvcs_cod_pgvcs";
                    Query resulPgSeguridad = entityManager.createNativeQuery(sqlConsulPgSeguri);
                    resulPgSeguridad.setParameter("cliac_ide_clien", clienIdenti);
                    resulPgSeguridad.setParameter("cliac_usu_virtu", cliacUsuVirtu);
                    String pgvcs_des_pgvcs = (String) resulPgSeguridad.getSingleResult();
                    allData.put("apellidos", clienApellidos );
                    allData.put("nombres", clienNombres);
                    allData.put("email", clienEmail);
                    allData.put("celular", clienNumero);
                    allData.put("fecha", clienFechaNac);
                    allData.put("PreguntaSeg", pgvcs_des_pgvcs);
                    allDataList.add(allData);
                    response.put("AllData", allDataList);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }else{
                allData.put("message", "Error en encontrar su informacion con el numero de cedula ingresado. ");
                allData.put("status", "DU10");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);

            }
            return new ResponseEntity<>(response, status);

        }catch (Exception e){
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
    public ResponseEntity<Map<String, Object>> validarRspSeg(HttpServletRequest request, ValidacionDatos validacionDatos) {
        try {
            String cliacUsuVirtu = (String) request.getAttribute("CliacUsuVirtu");
            String clienIdenti = (String) request.getAttribute("ClienIdenti");
            String numSocio = (String) request.getAttribute("numSocio");
            Map<String, Object> allData = new HashMap<>();
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> allDataList = new ArrayList<>();

            if (cliacUsuVirtu == null || clienIdenti == null || numSocio == null) {
                allData.put("message", "Datos del token incompletos");
                allData.put("status", "DU04");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            String sqlDatosUsuario = "SELECT cliac_pgt_2 FROM cnxclien, cnxcliac " +
                    "WHERE clien_ide_clien = :clien_ide_clien AND clien_cod_clien = :clien_cod_clien AND cliac_usu_virtu = :cliac_usu_virtu AND clien_ide_clien = cliac_ide_clien";
            Query resulDatosVerificadoUser = entityManager.createNativeQuery(sqlDatosUsuario);
            resulDatosVerificadoUser.setParameter("clien_ide_clien", clienIdenti);
            resulDatosVerificadoUser.setParameter("clien_cod_clien", numSocio);
            resulDatosVerificadoUser.setParameter("cliac_usu_virtu", cliacUsuVirtu);
            String pgvcs_des_pgvcs = (String) resulDatosVerificadoUser.getSingleResult();
            if(pgvcs_des_pgvcs.equals(validacionDatos.getRespSeguridad())){
                allData.put("message: ", "SI COINCIDE LA RESPUESTA CON LO DE LA BASE DE DATOS " );
                allData.put("status: ", "PGTSGOK" );
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }else{
                allData.put("message", "La contraseña no coincide con el registro realizado por el usuario. ");
                allData.put("status", "DU10");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        }catch (Exception e){
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


    public ResponseEntity<Map<String, Object>> cambioContrasenaOk(HttpServletRequest request, CambioContrasena cambioContrasena) {
        String cliacUsuVirtu = (String) request.getAttribute("CliacUsuVirtu");
        String clienIdenti = (String) request.getAttribute("ClienIdenti");
        String numSocio = (String) request.getAttribute("numSocio");
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> allData = new HashMap<>();
        List<Map<String, Object>> allDataList = new ArrayList<>();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // Validar coincidencia de la nueva contraseña y su confirmación
        if (!cambioContrasena.getPassNew().equals(cambioContrasena.getConfPassNew())) {
            allData.put("message", "La nueva contraseña y su confirmación no coinciden.");
            allData.put("status", "AA09");
            allData.put("errors", "No se aceptan contraseñas diferentes en el cambio de contraseña. ");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        }
        // Validar longitud de la nueva contraseña
        if (cambioContrasena.getPassNew().length() <= 8) {
            allData.put("message", "La contraseña debe tener al menos 8 caracteres. Por favor, inténtelo nuevamente.");
            allData.put("status", "AA10");
            allData.put("errors", "No se aceptan contraseña menores a 8 caractreres ");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (cambioContrasena.getPassNew().isEmpty()) {
            allData.put("message", "No puede dejar espacios en blanco en la nueva contraseña.");
            allData.put("status", "AA11");
            allData.put("errors", "No se aceptan espacios en blanco en la contraseña. ");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        }
        if (cambioContrasena.getConfPassNew().isEmpty()) {
            allData.put("message", "No puede dejar espacios en blanco en la confirmacion de la contraseña.");
            allData.put("status", "AA12");
            allData.put("errors", "No se aceptan espacios en blanco en la contraseña. ");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        // Validación para letras mayúsculas
        if (!cambioContrasena.getPassNew().matches(".*[A-Z].*")) {
            allData.put("message", "La contraseña debe contener al menos una letra mayúscula.");
            allData.put("status", "AA12");
            allData.put("errors", "Falta letra mayúscula en la contraseña.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Validación para letras minúsculas
        if (!cambioContrasena.getPassNew().matches(".*[a-z].*")) {
            allData.put("message", "La contraseña debe contener al menos una letra minúscula.");
            allData.put("status", "AA13");
            allData.put("errors", "Falta letra minúscula en la contraseña.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Validación para números
        if (!cambioContrasena.getPassNew().matches(".*[0-9].*")) {
            allData.put("message", "La contraseña debe contener al menos un número.");
            allData.put("status", "AA14");
            allData.put("errors", "Falta número en la contraseña.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Validación para caracteres especiales
        if (!cambioContrasena.getPassNew().matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            allData.put("message", "La contraseña debe contener al menos un carácter especial.");
            allData.put("status", "AA15");
            allData.put("errors", "Falta carácter especial en la contraseña.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        // Validación de contraseña actual no vacía
        if (cambioContrasena.getPassActual().isEmpty()) {
            allData.put("message", "La contraseña actual no puede estar vacía.");
            allData.put("status", "AA17");
            allData.put("errors", "Contraseña actual requerida.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Validación de longitud exacta de 4 caracteres
        if (cambioContrasena.getPassActual().length() != 4) {
            allData.put("message", "La contraseña actual debe tener exactamente 4 dígitos.");
            allData.put("status", "AA18");
            allData.put("errors", "Longitud incorrecta de la contraseña actual.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Validación de solo números
        if (!cambioContrasena.getPassActual().matches("^[0-9]{4}$")) {
            allData.put("message", "La contraseña actual debe contener solo números.");
            allData.put("status", "AA19");
            allData.put("errors", "La contraseña actual solo puede contener dígitos numéricos.");
            allDataList.add(allData);
            response.put("AllData", allDataList);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        try {
            String claveTempActual = cambioContrasena.getPassActual();
            String sqlValDatos = "SELECT codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal FROM vircodaccess " +
                    "WHERE codaccess_codigo_temporal = :codaccess_codigo_temporal AND codaccess_estado = '1' AND codsms_codigo = '5'";
            Query resulValDatosCamPass = entityManager.createNativeQuery(sqlValDatos);
            resulValDatosCamPass.setParameter("codaccess_codigo_temporal", claveTempActual);
            List<Object[]> result = resulValDatosCamPass.getResultList();

            for (Object[] row3 : result) {
                String clienCedula = row3[0].toString().trim();
                String clienUsuario = row3[1].toString().trim();
                String clienCodTemValido = row3[2].toString().trim();

                if (cambioContrasena.getPassActual().equals(clienCodTemValido) && clienIdenti.equals(clienCedula) && cliacUsuVirtu.equals(clienUsuario)) {
                    if (cambioContrasena.getPassNew().equals(cambioContrasena.getConfPassNew())) {
                        try {
                            String newPass = cambioContrasena.getPassNew();
                            PassSecure encriptarPass = new PassSecure();
                            String NewPassEncrip = encriptarPass.encryptPassword(newPass);
                            Libs fechaHoraService = new Libs(entityManager);
                            String fechaActual = fechaHoraService.obtenerFechaYHora();

                            // Actualizar contraseñas anteriores a estado 0
                            String sqlUpdateEstadoPass = "UPDATE virwwwpswdcambio SET virwwwpswdcambio_estado = '0' WHERE virwwwpswdcambio_cedula = :virwwwpswdcambio_cedula AND virwwwpswdcambio_estado = '1' AND codsms_codigo = 5";
                            Query resultUpdateEstadoPass = entityManager.createNativeQuery(sqlUpdateEstadoPass);
                            resultUpdateEstadoPass.setParameter("virwwwpswdcambio_cedula", clienIdenti);
                            resultUpdateEstadoPass.executeUpdate();

                            // Insertar nueva contraseña
                            String sqlCambioPassTemp = "INSERT INTO virwwwpswdcambio (virwwwpswdcambio_cedula, virwwwpswdcambio_usuario, virwwwpswdcambio_socio, virwwwpswdcambio_estado, virwwwpswdcambio_newpass, virwwwpswdcambio_confpass, codsms_codigo, codaccess_fecha) " +
                                    "VALUES (:virwwwpswdcambio_cedula, :virwwwpswdcambio_usuario, :virwwwpswdcambio_socio, :virwwwpswdcambio_estado, :virwwwpswdcambio_newpass, :virwwwpswdcambio_confpass, :codsms_codigo, :codaccess_fecha)";
                            Query resulCambioPassTemp = entityManager.createNativeQuery(sqlCambioPassTemp);
                            resulCambioPassTemp.setParameter("virwwwpswdcambio_cedula", clienCedula);
                            resulCambioPassTemp.setParameter("virwwwpswdcambio_usuario", clienUsuario);
                            resulCambioPassTemp.setParameter("virwwwpswdcambio_socio", numSocio);
                            resulCambioPassTemp.setParameter("virwwwpswdcambio_estado", "1");
                            resulCambioPassTemp.setParameter("virwwwpswdcambio_newpass", NewPassEncrip);
                            resulCambioPassTemp.setParameter("virwwwpswdcambio_confpass", NewPassEncrip);
                            resulCambioPassTemp.setParameter("codsms_codigo", 5);
                            resulCambioPassTemp.setParameter("codaccess_fecha", fechaActual);
                            int resultado = resulCambioPassTemp.executeUpdate();

                            if (resultado > 0) {
                                // Obtener datos para envío de código
                                String sqlDatosCorreoIngreso = "SELECT clien_ape_clien, clien_nom_clien, clien_dir_email, clien_tlf_celul FROM cnxclien, cnxcliac " +
                                        "WHERE cliac_usu_virtu = :username AND clien_ide_clien = cliac_ide_clien";
                                Query resulDatosCorreoIngreso = entityManager.createNativeQuery(sqlDatosCorreoIngreso);
                                resulDatosCorreoIngreso.setParameter("username", cliacUsuVirtu);

                                List<Object[]> results2 = resulDatosCorreoIngreso.getResultList();
                                for (Object[] row2 : results2) {
                                    String clienApellidos = row2[0].toString().trim();
                                    String clienNombres = row2[1].toString().trim();
                                    String clienEmail = row2[2].toString().trim();
                                    String clienNumero = row2[3].toString().trim();

                                    String CodigoDesbloqueo = codigoAleatorio6Temp();

                                    Libs fechaHoraService1 = new Libs(entityManager);
                                    String FechaDesbloqueoUser = fechaHoraService1.obtenerFechaYHora();

                                    // Enviar SMS
                                    SendSMS smsDesbloqueo = new SendSMS();
                                    smsDesbloqueo.sendSecurityCodeSMS(clienNumero,"1150",CodigoDesbloqueo,"Actualizar su Clave de Acceso", FechaDesbloqueoUser);

                                    // Enviar correo
                                    sendEmail enviarCorreo = new sendEmail();
                                    enviarCorreo.sendEmailTokenTemp(clienApellidos, clienNombres, FechaDesbloqueoUser, clienEmail, CodigoDesbloqueo);

                                    // Actualizar estados anteriores a 0
                                    String sqlUpdateEstado = "UPDATE vircodaccess SET codaccess_estado = '0' WHERE codaccess_cedula = :codaccess_cedula AND codaccess_estado = '1' AND codsms_codigo = 5";
                                    Query resultUpdateEstado = entityManager.createNativeQuery(sqlUpdateEstado);
                                    resultUpdateEstado.setParameter("codaccess_cedula", clienIdenti);
                                    resultUpdateEstado.executeUpdate();

                                    // Insertar nuevo código temporal
                                    String sqlInsertToken = "INSERT INTO vircodaccess (codaccess_cedula, codaccess_usuario, codaccess_codigo_temporal, codsms_codigo, codaccess_estado, codaccess_fecha) " +
                                            "VALUES (:codaccess_cedula, :codaccess_usuario, :codaccess_codigo_temporal, :codsms_codigo, :codaccess_estado, :codaccess_fecha)";
                                    Query resultInsertTokenAcceso = entityManager.createNativeQuery(sqlInsertToken);
                                    resultInsertTokenAcceso.setParameter("codaccess_cedula", clienIdenti);
                                    resultInsertTokenAcceso.setParameter("codaccess_usuario", clienUsuario);
                                    resultInsertTokenAcceso.setParameter("codaccess_codigo_temporal", CodigoDesbloqueo);
                                    resultInsertTokenAcceso.setParameter("codsms_codigo", 5);
                                    resultInsertTokenAcceso.setParameter("codaccess_estado", "1");
                                    resultInsertTokenAcceso.setParameter("codaccess_fecha", FechaDesbloqueoUser);
                                    resultInsertTokenAcceso.executeUpdate();
                                    tokenExpirationService.programarExpiracionToken(clienCedula, CodigoDesbloqueo, "5");

                                }
                                allData.put("message", "PASA ENVÍO CÓDIGO DE 6 CARACTERES OK!");
                                allData.put("status", "CC100");
                                allDataList.add(allData);
                                response.put("AllData", allDataList);
                                return new ResponseEntity<>(response, HttpStatus.OK);
                            }
                        } catch (Exception e) {
                            System.out.println("Error al insertar: " + e.getMessage());
                            allData.put("message", "Error al actualizar la contraseña");
                            allData.put("status", "ERROR");
                            allData.put("errors", e.getMessage());
                            allDataList.add(allData);
                            response.put("AllData", allDataList);
                            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } else {
                        allData.put("message", "Las contraseñas ingresadas no coinciden, intente de nuevo.");
                        allData.put("status", "CC02");
                        allData.put("errors", "Ambas contraseñas deben ser iguales para poder continuar");
                        allDataList.add(allData);
                        response.put("AllData", allDataList);
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }
                } else {
                    allData.put("message", "La información ingresada no coincide con la información registrada en la BDD");
                    allData.put("status", "CC01");
                    allData.put("errors", "Error al procesar la información para cambio de contraseña");
                    allDataList.add(allData);
                    response.put("AllData", allDataList);
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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

    public ResponseEntity<Map<String, Object>> validarCodigoSeguFinal(HttpServletRequest request, CodSegurdiad codSeguridad) {
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
                allData.put("status", "AA022");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
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
                    "WHERE codaccess_cedula = :codaccess_cedula AND codaccess_usuario = :codaccess_usuario AND codaccess_estado = :codaccess_estado  AND codsms_codigo =:codsms_codigo ";
            Query queryVerificaTokenBDD = entityManager.createNativeQuery(sqlVerificaTokenBDD);
            queryVerificaTokenBDD.setParameter("codaccess_cedula", clienIdenti);
            queryVerificaTokenBDD.setParameter("codaccess_usuario", cliacUsuVirtu);
            queryVerificaTokenBDD.setParameter("codaccess_estado", "1");
            queryVerificaTokenBDD.setParameter("codsms_codigo", 5);
            List<Object[]> resultsTokenBDD = queryVerificaTokenBDD.getResultList();
            if (!resultsTokenBDD.isEmpty()) {
                String tokenFromDB = (String) queryVerificaTokenBDD.getSingleResult();
                if (tokenFromDB != null && codSeguridad.getCodaccess_codigo_temporal() != null &&
                        codSeguridad.getCodaccess_codigo_temporal().equals(tokenFromDB.trim())) {

                    String sqlExtraerPassBDD = "SELECT virwwwpswdcambio_newpass FROM virwwwpswdcambio " +
                            "WHERE virwwwpswdcambio_cedula = :virwwwpswdcambio_cedula " +
                            "AND virwwwpswdcambio_usuario = :virwwwpswdcambio_usuario " +
                            "AND virwwwpswdcambio_estado = :virwwwpswdcambio_estado " +
                            "AND codsms_codigo = :codsms_codigo";

                    Query queryVerificaPassBDD = entityManager.createNativeQuery(sqlExtraerPassBDD);
                    queryVerificaPassBDD.setParameter("virwwwpswdcambio_cedula", clienIdenti);
                    queryVerificaPassBDD.setParameter("virwwwpswdcambio_usuario", cliacUsuVirtu);
                    queryVerificaPassBDD.setParameter("virwwwpswdcambio_estado", "1");
                    queryVerificaPassBDD.setParameter("codsms_codigo", 5);

                    List<String> passBDD = queryVerificaPassBDD.getResultList();

                    if (!passBDD.isEmpty()) {
                        String newPass = (String) passBDD.get(0);
                        System.out.println("virwwwpswdcambio_newpass: " + newPass);
                        // Construir la sentencia de actualización
                        String sqlUpdatePassword = "UPDATE cnxclien SET clien_www_pswrd = :newPassword " +
                                "WHERE clien_ide_clien = :clientId";
                        // Crear la consulta de actualización
                        Query queryUpdatePassword = entityManager.createNativeQuery(sqlUpdatePassword);
                        // Configurar los parámetros
                        queryUpdatePassword.setParameter("newPassword", newPass);
                        queryUpdatePassword.setParameter("clientId", clienIdenti);
                        // Ejecutar la actualización
                        int rowsUpdated = queryUpdatePassword.executeUpdate();

                        if (rowsUpdated > 0) {
                            allData.put("message", "CONTRASEÑA ACTUALIZADA CORRECTAMENTE");
                            allData.put("status", "CCOK");
                            allDataList.add(allData);
                            response.put("AllData", allDataList);
                            return new ResponseEntity<>(response, HttpStatus.OK);
                        } else {
                            System.out.println("No se pudo actualizar la contraseña. Verifica los datos.");
                        }
                    } else {
                        System.out.println("No se encontraron resultados para virwwwpswdcambio_newpass.");
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
                                    emailBloq.sendEmailBloqueo(clienApellidos, clienNombres, FechaHora, clienEmail, IpIngreso);
                                    intentosRealizadoTokenFallos = 0;
                                    response.put("success", false);
                                    response.put("message", "Usuario bloqueado por exceder límite de intentos");
                                    response.put("status", "AA025");
                                }
                            }
                        } catch (Exception e) {
                            response.put("success", false);
                            response.put("message", "Error al intentar bloquear el usuario");
                            response.put("status", "AA024");
                        }
                    } else {
                        response.put("success", false);
                        response.put("message", "Token incorrecto. Intentos restantes: " + (4 - intentosRealizadoTokenFallos));
                        response.put("status", "AA023");
                    }
                }
            }else {
                allData.put("status", "AA027");
                allData.put("errors", "Codigo temporal expirado por exceder el tiempo limite de 4 minutos. ");
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


    public String validarCamposBlanco(CambioPassUser credencialPassUser){
        if(credencialPassUser.getCliacUsuVirtu() == null || credencialPassUser.getCliacUsuVirtu().isEmpty()){
            return "El usuario no puede estar en blanco";

        }
        if(credencialPassUser.getClienIdeClien() == null || credencialPassUser.getClienIdeClien().isEmpty()){
            return "El numero de indentificacion no puede estar en blanco";

        }
        if(credencialPassUser.getClienCodClien() == null || credencialPassUser.getClienCodClien().isEmpty()){
            return  "El numero de socio no puede estar en blanco";
        }
        if(credencialPassUser.getFechaNacimiento() == null || credencialPassUser.getFechaNacimiento().isEmpty()
        ){
            return "La fecha de nacimiento no puede estar en blanco";
        }

        return null;
    }
    public String validarCedulaEC(CambioPassUser request) {
        String cedula = request.getClienIdeClien();

        if (cedula == null || cedula.length() != 10 || !cedula.matches("\\d{10}")) {
            return "Cédula inválida: debe contener 10 dígitos";
        }

        int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;
        int digito;

        for (int i = 0; i < 9; i++) {
            digito = Character.getNumericValue(cedula.charAt(i)) * coeficientes[i];
            if (digito > 9) {
                digito -= 9;
            }
            suma += digito;
        }

        int verificador = 10 - (suma % 10);
        if (verificador == 10) {
            verificador = 0;
        }

        if (verificador != Character.getNumericValue(cedula.charAt(9))) {
            return "Cédula inválida: dígito verificador incorrecto";
        }

        return null;
    }
    public String codigoAleatorio6Temp() {
        // Genera un número aleatorio de 6 dígitos
        Random random = new Random();
        int numeroAleatorio = 100000 + random.nextInt(900000); // Asegura 6 dígitos
        return String.valueOf(numeroAleatorio);
    }
    public String codigoAleatorioTemp() {
        // Genera un número aleatorio de 4 dígitos
        Random random = new Random();
        int numeroAleatorio = 1000 + random.nextInt(9000); // Asegura 4 dígitos
        return String.valueOf(numeroAleatorio);
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


}
