package com.ApiVirtualT.ApiVirtual.apiDashboard.controllers;

import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.TransferenciasDTO;
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
    public ResponseEntity<Map<String, Object>> srtGrabarDir(@RequestBody TransferenciasDTO dto, HttpServletRequest token) {
        return transferenciasService.srtGrabarDir(token, dto);
    }
    @PostMapping("/codTempDirectas")
    public ResponseEntity<Map<String, Object>> codTempDirectas(HttpServletRequest token, @RequestBody TransferenciasDTO dto){
        return transferenciasService.genCodDirectas(token, dto);
    }

    //Transferencias interbancarias
    @PostMapping("/srtGrabarInterbn")
    public ResponseEntity<Map<String, Object>> srtGrabarInterbn(HttpServletRequest token, @RequestBody TransferenciasDTO dto){
        return transferenciasService.srtGrabarInterban(token, dto);
    }
    @PostMapping("/codTempInterbancarias")
    public ResponseEntity<Map<String, Object>> codTempInterbancarias(HttpServletRequest token, @RequestBody TransferenciasDTO dto){
        return transferenciasService.genCodInterbancarias(token, dto);
    }

    //PAGO TARJETAS
    @PostMapping("/codTempPgTarjetas")
    public ResponseEntity<Map<String, Object>> codTemppgTarjetas(HttpServletRequest token, @RequestBody TransferenciasDTO dto){
        return transferenciasService.genCodInterbancarias_PgTj(token, dto);
    }
    @PostMapping("/srtGrabarPgTarjetas")
    public ResponseEntity<Map<String, Object>> srtGrabarPgTarjetas(HttpServletRequest token, @RequestBody TransferenciasDTO dto){
        return transferenciasService.srtGrabarPgTarjetas(token, dto);
    }


}
