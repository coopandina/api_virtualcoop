package com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.UserCredentials;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
