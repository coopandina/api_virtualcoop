package com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers;

import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/verificar")
@RequiredArgsConstructor
public class VerifiController {

    @Autowired
    private AuthService authService;
    /**
     * Endpoint para verificar Token seguridad Login
     */
    @PostMapping(value = "/codigo_seguridad")
    public ResponseEntity<Map<String, Object>> valCodiSeguridad(
            HttpServletRequest request,
            @RequestBody CodSegurdiad codSeguridad
    ) {
        return authService.validarCodSeguridad(request, codSeguridad);
    }
}
