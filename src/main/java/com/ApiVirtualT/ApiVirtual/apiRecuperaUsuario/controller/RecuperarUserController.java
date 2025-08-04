package com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.controller;
import com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.dto.RecuperarUserRequest;
import com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.service.RecuperarUsuarioService;
import com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.DTO.DatosRegistro;
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


public class RecuperarUserController {
    @Autowired
    private RecuperarUsuarioService recuperarUsuarioService;

    @PostMapping(value = "recuperar_usuario")
    public ResponseEntity<Map<String, Object>> recuperarUsuario (@RequestBody RecuperarUserRequest request){
        return recuperarUsuarioService.recuperarUsuarios(request);
    }



}
