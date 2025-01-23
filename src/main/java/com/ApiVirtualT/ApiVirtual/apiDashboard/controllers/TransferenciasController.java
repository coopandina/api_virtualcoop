package com.ApiVirtualT.ApiVirtual.apiDashboard.controllers;

import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.TransDirecDTO;
import com.ApiVirtualT.ApiVirtual.apiDashboard.services.TransferenciasDirecService;
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
@RequestMapping("/dashboard")
@RequiredArgsConstructor

public class TransferenciasController {

    @Autowired
    private TransferenciasDirecService transferenciasService;

    //Transferencias Directas
    @PostMapping("/srtGrabarDirectas")
    public ResponseEntity<Map<String, Object>> srtGrabarDir(@RequestBody TransDirecDTO dto, HttpServletRequest token) {
        return transferenciasService.srtGrabarDir(token, dto);
    }
    @PostMapping("/codTempDirectas")
    public ResponseEntity<Map<String, Object>> codTempDirectas(HttpServletRequest token, @RequestBody TransDirecDTO dto){
        return transferenciasService.genCodDirectas(token, dto);
    }

    //Transferencias interbancarias



}
