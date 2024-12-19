package com.ApiVirtualT.ApiVirtual.apiCambioPassword.controllers;


import com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO.CambioPassUser;
import com.ApiVirtualT.ApiVirtual.apiCambioPassword.services.CambioPassService;
import lombok.RequiredArgsConstructor;
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
public class CambioPassController {
    /**
     * Endpoint para cambiar contraseña de usuario
     */
    @Autowired
    private CambioPassService cambioPassService;
    @PostMapping(value = "cambio_contrasena")
    public ResponseEntity<Map<String, Object>> cambioPassword(@RequestBody CambioPassUser credencialesCambio){
        return cambioPassService.cambioPassUsuario(credencialesCambio);
    }
}
