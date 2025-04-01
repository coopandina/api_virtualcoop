package com.ApiVirtualT.ApiVirtual.apiCambioPassword.controllers;


import com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO.CambioContrasena;
import com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO.ValidacionDatos;
import com.ApiVirtualT.ApiVirtual.apiCambioPassword.services.CambioPassService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/verificar")
@RequiredArgsConstructor
public class CodPassTempController {
    @Autowired
    private CambioPassService cambioPassService;

    @PostMapping("codigo_cambio_contrasena")
    public ResponseEntity<Map<String, Object>> validarCodSegPass (HttpServletRequest request, @RequestBody CodSegurdiad codSeguridad){
        return cambioPassService.validarCodSeguridad(request, codSeguridad);
    }
    @PostMapping("verificar_datos")
    public ResponseEntity<Map<String, Object>> verificarDatos (HttpServletRequest request, @RequestBody ValidacionDatos validacionDatos){
        return cambioPassService.validarDatosUsuario(request, validacionDatos);
    }
    @PostMapping("/cambio_contrasenaOK")
    public ResponseEntity<Map<String, Object>> ContrasenaCambio (HttpServletRequest request,
                                                                 @RequestBody CambioContrasena cambioContrasena){
        return cambioPassService.cambioContrasenaOk(request, cambioContrasena);
    }

    @PostMapping("/codigo_seis_cambiopass")
    public ResponseEntity<Map<String, Object>> valCodiSeguridadDesbloqueo(
            HttpServletRequest request,
            @RequestBody CodSegurdiad codSeguridad
    ) {
        return cambioPassService.validarCodigoSeguFinal(request, codSeguridad);
    }
    @PostMapping("val_rpt_seguridad")
    public  ResponseEntity<Map<String, Object>> VerificarRespuesta (HttpServletRequest request, @RequestBody ValidacionDatos validacionDatos ){
        return cambioPassService.validarRspSeg(request, validacionDatos);
    }
}
