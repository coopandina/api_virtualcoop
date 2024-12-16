package com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers;

import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.UserCredentials;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CambioContrasenaCredencial;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {


    @Autowired
    private  AuthService authService;
    /**
     * Endpoint para login
     */

    @PostMapping(value = "login")
    public ResponseEntity<Map<String, Object>>accessLogin(@RequestBody UserCredentials request) {

        return authService.accesslogin(request);
    }



    /**
     * Endpoint para cambio de contraseña
     */
    @PostMapping(value = "cambiar_contrasena")
    public ResponseEntity<Map<String, Object>> cambiarContrasena(@RequestBody CambioContrasenaCredencial request){

        return authService.cambiarContrasena(request);
    }
}
