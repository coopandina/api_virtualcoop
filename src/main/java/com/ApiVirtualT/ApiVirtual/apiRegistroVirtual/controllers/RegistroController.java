package com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.controllers;


import com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.DTO.DatosRegistro;
import com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.services.RegistroService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistroController {
    @Autowired
    private RegistroService registroService;
    /**
     * Endpoint para registrar usuario
     */
    @PostMapping(value = "registro_usuario")
    public ResponseEntity<Map<String, Object>> registroUsuarios (@RequestBody DatosRegistro datosRegistro){
        return registroService.registrarUsuarios(datosRegistro);
    }







}
