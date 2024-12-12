package com.ApiVirtualT.ApiVirtual.apiMensaje.controller;


import com.ApiVirtualT.ApiVirtual.apiMensaje.service.MensajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ejemplo")
@RequiredArgsConstructor
public class MensajeController {
    @Autowired
    private MensajeService mensajeService;


    @PostMapping("/saludo")
    public String obtenerMensaje(){
        return mensajeService.obtenerMensaje();
    }



}
