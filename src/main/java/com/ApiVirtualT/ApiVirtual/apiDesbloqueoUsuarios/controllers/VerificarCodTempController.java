package com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.controllers;

import com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador.CodSegurdiad;
import com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO.CambioContrasena;
import com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO.ValidacionDatos;
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
public class VerificarCodTempController {
    @Autowired
    private DesbloqueoService desbloqueoService;

    @PostMapping("/codigo_desbloqueo")
    public ResponseEntity<Map<String, Object>> validarCodSegDesbloqueo(HttpServletRequest request, @RequestBody CodSegurdiad codSeguridad){
        System.out.println(codSeguridad.getCodaccess_codigo_temporal());
        return desbloqueoService.validarCodSeguridad(request, codSeguridad);
    }
    @PostMapping("/verificar_datos_desbloqueo")
    public ResponseEntity<Map<String, Object>> verificarDatos (HttpServletRequest request, @RequestBody ValidacionDatos validacionDatos){
        return desbloqueoService.validarDatosUsuario(request, validacionDatos);
    }
    @PostMapping("/cambio_contrasena")
    public ResponseEntity<Map<String, Object>> cambioContrasena (HttpServletRequest request,
                                                                 @RequestBody CambioContrasena cambioContrasena){
        return desbloqueoService.cambioContrasena(request, cambioContrasena);
    }

    @PostMapping("/codigo_fin_cambiopass")
    public ResponseEntity<Map<String, Object>> valCodiSeguridadDesbloqueo(
            HttpServletRequest request,
            @RequestBody CodSegurdiad codSeguridad
    ) {
        return desbloqueoService.validarCodSeguridadFinal(request, codSeguridad);
    }

}
