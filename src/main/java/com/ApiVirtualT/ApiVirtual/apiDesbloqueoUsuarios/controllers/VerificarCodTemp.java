package com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.controllers;

import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.DTO.DesbloqueoUser;
import com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.services.DesbloqueoService;
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
public class VerificarCodTemp {
    @Autowired
    private DesbloqueoService desbloqueoService;

    @PostMapping("/codigo_desbloqueo")
    public ResponseEntity<Map<String, Object>> validarCodSegDesbloqueo(HttpServletRequest request, @RequestBody CodSegurdiad codSeguridad){
        return desbloqueoService.validarCodSeguridad(request, codSeguridad);
    }

}
