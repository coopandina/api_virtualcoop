package com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.services;


import com.ApiVirtualT.ApiVirtual.apiAutenticacion.JWT.JwtUtil;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.DTO.CrearUsuario;
import com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.DTO.DatosRegistro;
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

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

import passwords.passwordSecureToken;
import sms.SendSMS;

@Transactional
@Service
@RequiredArgsConstructor
public class RegistroService {

    @PersistenceContext
    private EntityManager entityManager;
    public ResponseEntity<Map<String, Object>> registrarUsuarios(DatosRegistro datosRegistro){
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> allData = new HashMap<>();
        List<Map<String, Object>> allDataList = new ArrayList<>();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        try {
            // Validación del identificador (cédula/RUC/pasaporte)
            if (datosRegistro.getClienIdeClien() == null || datosRegistro.getClienIdeClien().trim().isEmpty()) {
                allData.put("message", "El número de identificación no puede estar vacío.");
                allData.put("status", "AA01");
                allData.put("errors", "Campo de identificación obligatorio.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validación de formato de identificación
            String identificacion = datosRegistro.getClienIdeClien().trim();
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
            if (datosRegistro.getFechaNacimiento() == null || datosRegistro.getFechaNacimiento().trim().isEmpty()) {
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
                LocalDate fechaNacimiento = LocalDate.parse(datosRegistro.getFechaNacimiento().trim(), formatter);
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

            // Validación de la clave de control
            if (datosRegistro.getClaveControl() == null || datosRegistro.getClaveControl().trim().isEmpty()) {
                allData.put("message", "La clave de control no puede estar vacía.");
                allData.put("status", "AA06");
                allData.put("errors", "Campo de clave de control obligatorio.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validación del formato de la clave de control (4 números exactos)
            if (!Pattern.matches("^[0-9]{4}$", datosRegistro.getClaveControl().trim())) {
                allData.put("message", "Formato de clave de control inválido.");
                allData.put("status", "AA07");
                allData.put("errors", "La clave de control debe contener exactamente 4 números.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String identificacionClien = datosRegistro.getClienIdeClien();
            String fechaNacClien = datosRegistro.getFechaNacimiento();
            String claveControlClie = datosRegistro.getClaveControl();
            String claveControlEncriptada =  passwordSecureToken.encryptPassword(claveControlClie);
            System.out.println(fechaNacClien);

            String sqlValidarDatos = "SELECT clien_ide_clien, clien_www_ctrpw, clien_fec_nacim FROM cnxclien " +
                    "WHERE clien_ide_clien = :clien_ide_clien AND clien_www_ctrpw = :clien_www_ctrpw AND clien_fec_nacim = TO_DATE(:clien_fec_nacim, '%d/%m/%Y')";
            Query resulValidarCampos = entityManager.createNativeQuery(sqlValidarDatos);
            resulValidarCampos.setParameter("clien_ide_clien", identificacionClien);
            resulValidarCampos.setParameter("clien_www_ctrpw", claveControlEncriptada);
            resulValidarCampos.setParameter("clien_fec_nacim", fechaNacClien);
            String sqlValCliac = """
                    SELECT cliac_ide_clien FROM cnxcliac WHERE cliac_ide_clien = :cliac_ide_clien
                    """;
            Query resultValCliac = entityManager.createNativeQuery(sqlValCliac);
            resultValCliac.setParameter("cliac_ide_clien", identificacionClien);
            List<?> resultadosCliac = resultValCliac.getResultList();

            List<?> resultados = resulValidarCampos.getResultList();
            SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
            for (Object resultado : resultados) {
                Object[] row = (Object[]) resultado;
                if (resultadosCliac.isEmpty()) {
                    String clienteId = row[0].toString().trim();
                    Date fechaNacimiento = (Date) row[2];
                    String fechaFormateada = formatoFecha.format(fechaNacimiento).trim();
                    System.out.println("ID Cliente: " + clienteId);
                    System.out.println("Fecha Nacimiento: " + fechaFormateada);
                    String tokenRegistro = JwtUtil.tokenRegistroVirtual(clienteId, fechaFormateada);
                    String sqlPgtSegu = "SELECT pgvcs_cod_pgvcs,pgvcs_des_pgvcs FROM andpgvcs ";
                    Query resulPgtSegu = entityManager.createNativeQuery(sqlPgtSegu);
                    List<Object[]> resultList = resulPgtSegu.getResultList();
                    List<Map<String, Object>> preguntasSeguridad = new ArrayList<>();
                    // Procesar cada resultado y agregarlo a la lista
                    for (Object[] result : resultList) {
                        Map<String, Object> pregunta = new HashMap<>();
                        pregunta.put("codigo", result[0]);
                        pregunta.put("pregunta", result[1]);
                        preguntasSeguridad.add(pregunta);
                    }
                    //Los datos existen
                    allData.put("preguntas_seguridad:" , preguntasSeguridad);
                    allData.put("message", "Validación exitosa");
                    allData.put("status", "CU1OK");
                    allData.put("token", tokenRegistro);
                    allDataList.add(allData);
                    response.put("AllData", allDataList);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    allData.put("error", "EL usuario ya se encuentra registrado :(");
                    allData.put("message", "EL usuario ya se encuentra registrado.");
                    allData.put("status", "CU007");
                    allDataList.add(allData);
                    response.put("AllData", allDataList);
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
            if (resultados.isEmpty()) {
                allData.put("message", "Datos incorrectos");
                allData.put("status", "AA08");
                allData.put("errors", "Los datos ingresados no coinciden con nuestros registros");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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

    public ResponseEntity<Map<String, Object>> ObtenerUsuarios(HttpServletRequest request, CrearUsuario crearUsuario) {
        try {
            String clienIdenti = (String) request.getAttribute("ClienIdenti");
            String fecNacClien = (String) request.getAttribute("fecNacClien");
            System.out.println(clienIdenti);
            System.out.println(fecNacClien);
            Map<String, Object> allData = new HashMap<>();
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> allDataList = new ArrayList<>();
            if (clienIdenti == null || fecNacClien == null) {
                allData.put("message", "Datos del token incompletos");
                allData.put("status", "DU04");
                allData.put("errors", "ERROR EN LA AUTENTICACIÓN");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            // Validar que no estén vacíos
            if (crearUsuario.getUsuario() == null || crearUsuario.getUsuario().trim().isEmpty()) {
                allData.put("message", "El usuario no puede estar vacío.");
                allData.put("status", "AA20");
                allData.put("errors", "Campo de usuario obligatorio.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (crearUsuario.getConfirmUsuario() == null || crearUsuario.getConfirmUsuario().trim().isEmpty()) {
                allData.put("message", "La confirmación de usuario no puede estar vacía.");
                allData.put("status", "AA21");
                allData.put("errors", "Campo de confirmación de usuario obligatorio.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validar longitud mínima
            if (crearUsuario.getUsuario().length() < 8 || crearUsuario.getUsuario().length() > 30) {
                allData.put("message", "El usuario debe tener al menos 8 caracteres o maximo 30 caracteres .");
                allData.put("status", "AA22");
                allData.put("errors", "Longitud mínima de usuario no cumplida.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (crearUsuario.getConfirmUsuario().length() < 8 || crearUsuario.getConfirmUsuario().length() > 30) {
                allData.put("message", "El usuario de configuracion debe tener al menos 8 caracteres o maximo 30 caracteres .");
                allData.put("status", "AA22");
                allData.put("errors", "Longitud mínima de usuario no cumplida.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validar que los usuarios coincidan
            if (!crearUsuario.getUsuario().equals(crearUsuario.getConfirmUsuario())) {
                allData.put("message", "Los usuarios no coinciden.");
                allData.put("status", "AA23");
                allData.put("errors", "El usuario y su confirmación deben ser idénticos.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validar mayúsculas
            if (!crearUsuario.getUsuario().matches(".*[A-Z].*")) {
                allData.put("message", "El usuario debe contener al menos una letra mayúscula.");
                allData.put("status", "AA24");
                allData.put("errors", "Falta letra mayúscula en el usuario.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validar minúsculas
            if (!crearUsuario.getUsuario().matches(".*[a-z].*")) {
                allData.put("message", "El usuario debe contener al menos una letra minúscula.");
                allData.put("status", "AA25");
                allData.put("errors", "Falta letra minúscula en el usuario.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validar números
            if (!crearUsuario.getUsuario().matches(".*\\d.*")) {
                allData.put("message", "El usuario debe contener al menos un número.");
                allData.put("status", "AA26");
                allData.put("errors", "Falta número en el usuario.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validar que no contenga espacios
            if (crearUsuario.getUsuario().contains(" ")) {
                allData.put("message", "El usuario no puede contener espacios.");
                allData.put("status", "AA28");
                allData.put("errors", "No se permiten espacios en el usuario.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validar que no estén vacías
            if (crearUsuario.getPassword() == null || crearUsuario.getPassword().trim().isEmpty()) {
                allData.put("message", "La contraseña no puede estar vacía.");
                allData.put("status", "AA30");
                allData.put("errors", "Campo de contraseña obligatorio.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (crearUsuario.getConfirPassword() == null || crearUsuario.getConfirPassword().trim().isEmpty()) {
                allData.put("message", "La confirmación de contraseña no puede estar vacía.");
                allData.put("status", "AA31");
                allData.put("errors", "Campo de confirmación de contraseña obligatorio.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validar longitud mínima y maxima
            if (crearUsuario.getPassword().length() < 8 || crearUsuario.getPassword().length() >60 ) {
                allData.put("message", "La contraseña debe tener al menos 8 caracteres y máximo 60 caracteres");
                allData.put("status", "AA32");
                allData.put("errors", "Longitud mínima de contraseña no cumplida.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (crearUsuario.getConfirPassword().length() < 8 || crearUsuario.getConfirPassword().length() >60 ) {
                allData.put("message", "La contraseña de confirmacion debe tener al menos 8 caracteres y máximo 60 caracteres");
                allData.put("status", "AA32");
                allData.put("errors", "Longitud mínima de contraseña no cumplida.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validar que las contraseñas coincidan
            if (!crearUsuario.getPassword().equals(crearUsuario.getConfirPassword())) {
                allData.put("message", "Las contraseñas no coinciden.");
                allData.put("status", "AA33");
                allData.put("errors", "La contraseña y su confirmación deben ser idénticas.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validar mayúsculas
            if (!crearUsuario.getPassword().matches(".*[A-Z].*")) {
                allData.put("message", "La contraseña debe contener al menos una letra mayúscula.");
                allData.put("status", "AA34");
                allData.put("errors", "Falta letra mayúscula en la contraseña.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validar minúsculas
            if (!crearUsuario.getPassword().matches(".*[a-z].*")) {
                allData.put("message", "La contraseña debe contener al menos una letra minúscula.");
                allData.put("status", "AA35");
                allData.put("errors", "Falta letra minúscula en la contraseña.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            // Validar números
            if (!crearUsuario.getPassword().matches(".*\\d.*")) {
                allData.put("message", "La contraseña debe contener al menos un número.");
                allData.put("status", "AA36");
                allData.put("errors", "Falta número en la contraseña.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (crearUsuario.getPassword() != null && !crearUsuario.getPassword().isEmpty() &&
                    !crearUsuario.getPassword().matches(".*[@#$%&*\\-+!?._].*")) {
                allData.put("message", "La contraseña debe contener al menos un carácter especial permitido (@, #, $, %, &, *, -, +, !, ?, ., _)");
                allData.put("status", "AA37");
                allData.put("errors", "Falta carácter especial permitido en la contraseña.");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String nomUsario = crearUsuario.getUsuario();
            System.err.println(nomUsario);
            String sqlValUsario = """
                    SELECT COUNT(*) AS total
                    FROM cnxcliac
                    WHERE LOWER(cliac_usu_virtu) = LOWER(:cliac_usu_virtu);
                    """;
            Query sqlValUsarios = entityManager.createNativeQuery(sqlValUsario);
            sqlValUsarios.setParameter("cliac_usu_virtu","'" + nomUsario + "'");

            Number countResult = (Number) sqlValUsarios.getSingleResult();
            int totalUsuarios = countResult.intValue(); // Convertir a entero

            System.err.println("Resultado de la consulta: " + totalUsuarios);

            if(totalUsuarios > 0){
                allData.put("status", "CU45ERROR");
                allData.put("errors", "Usuario no disponible !!!");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            String sqlValidarDatos = "SELECT clien_ide_clien,  clien_fec_nacim FROM cnxclien " +
                    "WHERE clien_ide_clien = :clien_ide_clien AND clien_fec_nacim = TO_DATE(:clien_fec_nacim, '%d/%m/%Y')";
            Query resulValidarCampos = entityManager.createNativeQuery(sqlValidarDatos);
            resulValidarCampos.setParameter("clien_ide_clien", clienIdenti);
            resulValidarCampos.setParameter("clien_fec_nacim", fecNacClien);

            String usuarioCliac = crearUsuario.getUsuario();
            String pregSeguridad = crearUsuario.getPregunSeguridad();
            String respSeeguridad = crearUsuario.getRespPregunSeguridad();
            Integer terCondiciones = crearUsuario.getCliacTermCondic();
            String password = crearUsuario.getPassword().trim();
            PassSecure pass = new PassSecure();
            String passEncriptada = pass.encryptPassword(password);



            List<?> resultados = resulValidarCampos.getResultList();
            if (!resultados.isEmpty()) {
                String sqlValidarExiCncliac = "SELECT * FROM cnxcliac " +
                        "WHERE cliac_ide_clien = :clien_ide_clien";
                Query resulValidarUseVir = entityManager.createNativeQuery(sqlValidarExiCncliac);
                resulValidarUseVir.setParameter("clien_ide_clien", clienIdenti);
                List<?> existeUsuario = resulValidarUseVir.getResultList();
                if(existeUsuario.isEmpty()){
                    String sqlInsertCnxcliac = "INSERT INTO cnxcliac " +
                            "(cliac_ide_clien, cliac_ctr_actua, cliac_num_diaac, cliac_ctr_bloq, cliac_ctr_condi, " +
                            "cliac_pgt_1, cliac_pgt_2, cliac_usu_virtu, cliac_ter_condi) " +
                            "VALUES (:cliac_ide_clien, :cliac_ctr_actua, :cliac_num_diaac, :cliac_ctr_bloq, :cliac_ctr_condi, " +
                            ":cliac_pgt_1, :cliac_pgt_2, :cliac_usu_virtu, :cliac_ter_condi)";
                    Query insertQuery = entityManager.createNativeQuery(sqlInsertCnxcliac);
                    insertQuery.setParameter("cliac_ide_clien", clienIdenti);
                    insertQuery.setParameter("cliac_ctr_actua", 1);
                    insertQuery.setParameter("cliac_num_diaac", 1);
                    insertQuery.setParameter("cliac_ctr_bloq", "1");
                    insertQuery.setParameter("cliac_ctr_condi", "1");
                    insertQuery.setParameter("cliac_pgt_1", pregSeguridad);
                    insertQuery.setParameter("cliac_pgt_2", respSeeguridad);
                    insertQuery.setParameter("cliac_usu_virtu", usuarioCliac);
                    insertQuery.setParameter("cliac_ter_condi", terCondiciones);
                    insertQuery.executeUpdate();

                    String sqlInserPassword = "UPDATE cnxclien SET clien_www_pswrd = :clien_www_pswrd " +
                            "WHERE clien_ide_clien = :clien_ide_clien ";
                    Query sqlResulUpdatPass = entityManager.createNativeQuery(sqlInserPassword);
                    sqlResulUpdatPass.setParameter("clien_www_pswrd", passEncriptada);
                    sqlResulUpdatPass.setParameter("clien_ide_clien", clienIdenti);
                    sqlResulUpdatPass.executeUpdate();

                    allData.put("status", "CU2OK");
                    allData.put("errors", "REGISTRO EXITOSO !!");
                    allDataList.add(allData);
                    response.put("AllData", allDataList);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }else{
                    allData.put("status", "BAD01");
                    allData.put("errors", "EL USUARIO YA ESTA REGISTRADO");
                    allDataList.add(allData);
                    response.put("AllData", allDataList);
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            } else {
                allData.put("status", "DU09");
                allData.put("errors", "TOKEN INCORRECTO");
                allDataList.add(allData);
                response.put("AllData", allDataList);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
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
}
