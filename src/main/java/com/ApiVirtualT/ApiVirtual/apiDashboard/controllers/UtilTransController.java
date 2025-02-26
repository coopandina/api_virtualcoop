package com.ApiVirtualT.ApiVirtual.apiDashboard.controllers;

import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.InterbancariasDTO;
import com.ApiVirtualT.ApiVirtual.apiDashboard.DTO.VerMovimientoCta;
import com.ApiVirtualT.ApiVirtual.apiDashboard.services.UtilsTransService;
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
public class UtilTransController {
    @Autowired
    private UtilsTransService utilsTransService;


    //Generar codigo qr

    @PostMapping("/generarQr")
    public ResponseEntity<Map<String, Object>> genQrCode(HttpServletRequest token){
        return utilsTransService.genQRCode(token);
    }

    //Validar tarjeta de credito
    @PostMapping("/validarTjCredit")
    public ResponseEntity<Map<String, Object>> validarTarjetas(HttpServletRequest token, @RequestBody InterbancariasDTO dto){
        return utilsTransService.valTarjetas(token, dto);
    }

    //Endpoint desencriptar
    @PostMapping("/descrypt")
    public ResponseEntity<Map<String, Object>> descrypt(HttpServletRequest token, @RequestBody InterbancariasDTO dto){
        return utilsTransService.descrypt(token, dto);
    }
    //Endpoint encriptar
    @PostMapping("/encrypt")
    public ResponseEntity<Map<String, Object>> encrypt(HttpServletRequest token, @RequestBody InterbancariasDTO dto){
        return utilsTransService.encrypt(token, dto);
    }






    //Transferencias Directas
    @PostMapping("/listCtaBeneDirectas")
    public ResponseEntity<Map<String, Object>> listBeneDirectos(HttpServletRequest token){
        return utilsTransService.listarCtaBenefDirectos(token);
    }
    @PostMapping("/infoCuentas")
    public ResponseEntity<Map<String, Object>> informacionCuentas(HttpServletRequest token){
        return utilsTransService.verInfCuenta(token);
    }

    @PostMapping("/verMovimientos")
    public ResponseEntity<Map<String, Object>> obtenerMovimientos(@RequestBody VerMovimientoCta dto, HttpServletRequest token) {
        return utilsTransService.obtenerMovimientos(dto, token);

    }
    @PostMapping("/buscarBeneficiario")
    public ResponseEntity<Map<String, Object>> buscarPorNombreApellido(@RequestBody VerMovimientoCta dto, HttpServletRequest token) {
        return utilsTransService.buscarPorNombreApellido(token, dto);

    }
    @PostMapping("/validarBenefDirecto")
    public ResponseEntity<Map<String, Object>> validarBeneDirecto(@RequestBody VerMovimientoCta dto, HttpServletRequest token) {
        return utilsTransService.validarBeneficiario(token, dto);
    }
    @PostMapping("/guardarbeneficiario")
    public ResponseEntity<Map<String, Object>> guardarBeneficiarioDirecto(@RequestBody VerMovimientoCta dto, HttpServletRequest token) {
        return utilsTransService.guardarBenefiDirecto(token, dto);
    }
    @PostMapping("/endPointValCtaTrans")
    public ResponseEntity<Map<String, Object>> validarCtaTransEstado(@RequestBody VerMovimientoCta dto, HttpServletRequest token) {
        return utilsTransService.validarCtaTransEstado(token, dto);
    }
    @PostMapping("/eliminarBenDirecto")
    public ResponseEntity<Map<String, Object>> elimBeneDirecto(HttpServletRequest token, @RequestBody VerMovimientoCta dto){
        return utilsTransService.eliminarBeneDirecto(token, dto);
    }
    @PostMapping("/listarbeneficiarios")
    public ResponseEntity<Map<String, Object>> litarCtaBeneficiarios(HttpServletRequest token){
        return utilsTransService.listarCtaBeneficiarios(token);
    }
    //Transferencias interbancarias
    @PostMapping("/listCtaDebitInter")
    public ResponseEntity<Map<String,Object>>listCuentBeneficiarios(HttpServletRequest token){
        return utilsTransService.lisCtaTransferibles(token);
    }
    @PostMapping("/lisBeneficiInterb")
    public ResponseEntity<Map<String,Object>>lisBeneficiariosInter(HttpServletRequest token){
        return utilsTransService.lisBeneInterbanc(token);
    }
    @PostMapping("/buscarCuentasInterba")
    public ResponseEntity<Map<String,Object>>buscarCtaInterbancaria(HttpServletRequest token, @RequestBody VerMovimientoCta dto){
        return utilsTransService.buscarCuentaInterbancaria(token, dto);
    }
    @PostMapping("/listarInsFinancieras")
    public ResponseEntity<Map<String,Object>>listarInstituciones(HttpServletRequest token){
        return utilsTransService.listarInstFinancieras(token);
    }
    @PostMapping("/guardarbenefiInterbancario")
    public ResponseEntity<Map<String, Object>> guardarBeneInterbancario(HttpServletRequest token, @RequestBody InterbancariasDTO dto) {
        return utilsTransService.guardarBeneInterbancario(token, dto);
    }
    @PostMapping("/buscarInstituBancaria")
    public ResponseEntity<Map<String, Object>> buscarInstitucionFinan(HttpServletRequest token, @RequestBody VerMovimientoCta dto) {
        return utilsTransService.buscarInstiFinanciera(token, dto);
    }

    //Pago tarjetas de credito
    @PostMapping("/ctaDebitarTarjetas")
    public ResponseEntity<Map<String, Object>> ctaDebitarPagTarjetas(HttpServletRequest token) {
        return utilsTransService.ctaDebitarPagTarjetas(token);
    }
    @PostMapping("/listTarjetasBeneficiarios")
    public ResponseEntity<Map<String, Object>> listTjBeneficiarios(HttpServletRequest token) {
        return utilsTransService.listBeneficiariosTarjetas(token);
    }
        @PostMapping("/guardarTarjeta")
    public ResponseEntity<Map<String, Object>> guardarTarjetaPago(HttpServletRequest token, @RequestBody InterbancariasDTO dto) {
        return utilsTransService.guardarTarjetas(token, dto);
    }
    @PostMapping("/eliminarTarjeta")
    public ResponseEntity<Map<String, Object>> eliminarTarjeta(HttpServletRequest token, @RequestBody InterbancariasDTO dto) {
        return utilsTransService.eliminarTarjetaPj(token, dto);
    }


}
