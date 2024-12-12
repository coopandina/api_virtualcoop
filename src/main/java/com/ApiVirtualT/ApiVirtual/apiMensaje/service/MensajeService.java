package com.ApiVirtualT.ApiVirtual.apiMensaje.service;


import org.springframework.stereotype.Service;

@Service
public class MensajeService {
    public String obtenerMensaje(){
        return "Hola servicio desplegado con exito";
    }
}
