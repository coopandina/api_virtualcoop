package com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.controller;


import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.AuthService;
import com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.dto.CodSegRequest;
import com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.service.RecuperarUsuarioService;
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

public class VerificacionUsuario {
    @Autowired
    private RecuperarUsuarioService recuperarUsuarioService;
    /**
     * Endpoint para verificar Token Recuperar usuario
     */
    @PostMapping(value = "/codigo_seguridad_usuario")
    public ResponseEntity<Map<String, Object>> valcodSegUser(HttpServletRequest request, @RequestBody CodSegRequest codSeguridad) {
        return recuperarUsuarioService.validarCodSeguridad(request, codSeguridad);
    }



}
