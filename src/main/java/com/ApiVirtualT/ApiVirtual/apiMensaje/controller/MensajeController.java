package com.ApiVirtualT.ApiVirtual.apiMensaje.controller;


import com.ApiVirtualT.ApiVirtual.apiMensaje.service.MensajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MensajeController {
    @Autowired
    private MensajeService mensajeService;


    @GetMapping("/saludo")
    public String obtenerMensaje(){
        return mensajeService.obtenerMensaje();
    }

}
