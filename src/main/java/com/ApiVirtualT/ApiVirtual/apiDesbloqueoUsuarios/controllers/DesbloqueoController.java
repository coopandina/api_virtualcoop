package com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.controllers;


import com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.DTO.DesbloqueoUser;
import com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.services.DesbloqueoService;
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
public class DesbloqueoController {

    @Autowired
    private DesbloqueoService desbloqueoService;
    /**
     * Endpoint para desbloqueo de usuarios
     */
    @PostMapping(value = "desbloqueo_usuario")
    public ResponseEntity<Map<String, Object>> desbloqueoUsuarios(@RequestBody DesbloqueoUser credencialesDesbloqueo){
        return desbloqueoService.desbloquearUsuario(credencialesDesbloqueo);
    }

}
