package com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.controllers;


import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.DTO.CrearUsuario;
import com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.services.RegistroService;
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
@RequestMapping("verificar")
@RequiredArgsConstructor
public class CreacionRegistroController {
    @Autowired
    private RegistroService registroService;

    @PostMapping("crear_usuario")
    public ResponseEntity<Map<String, Object>> validarCodSegDesbloqueo(HttpServletRequest request, @RequestBody CrearUsuario crearUsuario){
        return registroService.ObtenerUsuarios(request, crearUsuario);
    }
}
