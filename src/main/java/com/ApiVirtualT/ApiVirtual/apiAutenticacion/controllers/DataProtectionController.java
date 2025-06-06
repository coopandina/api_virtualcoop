package com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers;

import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.DataProtectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/terminos")
public class DataProtectionController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataProtectionController.class);

    @Autowired
    private DataProtectionService dataProtectionService;

    /**
     * Endpoint para verificar el estado de aceptación de términos
     * @param request Objeto HttpServletRequest que contiene el token
     * @return ResponseEntity con el estado de aceptación
     */
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> verificarEstadoTerminos(HttpServletRequest request) {
        logger.info("Iniciando verificación de estado de términos y condiciones");
        return dataProtectionService.verificarEstadoTerminos(request);
    }

    /**
     * Endpoint para registrar o actualizar la aceptación de términos
     * @param request Objeto HttpServletRequest que contiene el token
     * @param requestBody Cuerpo de la petición con el estado (0 o 1)
     * @return ResponseEntity con el resultado de la operación
     */
    @PostMapping("/registrar")
    public ResponseEntity<Map<String, Object>> registrarAceptacionTerminos(
            HttpServletRequest request,
            @RequestBody Map<String, String> requestBody) {

        logger.info("Iniciando registro de aceptación de términos");

        // Validación del cuerpo de la petición
        if (requestBody == null || !requestBody.containsKey("estado")) {
            logger.error("Request body inválido o falta campo 'estado'");
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El campo 'estado' es requerido (0 o 1)",
                    "status", 400
            ));
        }

        String estado = requestBody.get("estado");

        // Validación del valor del estado
        if (!"0".equals(estado) && !"1".equals(estado)) {
            logger.error("Estado inválido recibido: {}", estado);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Estado inválido. Debe ser 0 (no aceptado) o 1 (aceptado)",
                    "status", 400
            ));
        }

        logger.debug("Estado recibido válido: {}", estado);
        return dataProtectionService.registrarAceptacionTerminos(request, estado);
    }
}