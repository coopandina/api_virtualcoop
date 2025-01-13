package com.ApiVirtualT.ApiVirtual.apiDashboard.controllers;


import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.TransDirecDTO;
import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.verMovimientoCta;
import com.ApiVirtualT.ApiVirtual.apiDashboard.services.Dashservice;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class beneficiariosController {
    @Autowired
    private Dashservice dashservice;
    @PostMapping("/listarbeneficiarios")
    public ResponseEntity<Map<String, Object>> litarCtaBeneficiarios(HttpServletRequest token){
        return dashservice.listarCtaBeneficiarios(token);
    }
    @PostMapping("/infoCuentas")
    public ResponseEntity<Map<String, Object>> informacionCuentas(HttpServletRequest token){
        return dashservice.verInfCuenta(token);
    }

    @PostMapping("/verMovimientos")
    public ResponseEntity<Map<String, Object>> obtenerMovimientos(@RequestBody verMovimientoCta dto, HttpServletRequest token) {
        return dashservice.obtenerMovimientos(dto, token);

    }
    @PostMapping("/buscarBeneficiario")
    public ResponseEntity<Map<String, Object>> buscarPorNombreApellido(@RequestBody verMovimientoCta dto, HttpServletRequest token) {
        return dashservice.buscarPorNombreApellido(token, dto);

    }
    @PostMapping("/validarBenefDirecto")
    public ResponseEntity<Map<String, Object>> validarBeneDirecto(@RequestBody verMovimientoCta dto, HttpServletRequest token) {
        return dashservice.validarBeneficiario(token, dto);
    }
    @PostMapping("/guardarbeneficiario")
    public ResponseEntity<Map<String, Object>> guardarBeneficiarioDirecto(@RequestBody verMovimientoCta dto, HttpServletRequest token) {
        return dashservice.guardarBenefiDirecto(token, dto);
    }
    @PostMapping("/srtGrabarDirectas")
    public ResponseEntity<Map<String, Object>> srtGrabarDir(@RequestBody TransDirecDTO dto, HttpServletRequest token) {
        return dashservice.srtGrabarDir(token, dto);
    }

}
