package com.ApiVirtualT.ApiVirtual.apiDashboard.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterbancariasDTO {

    String tipIdentiCta;
    String numIdentifiacion;
    String nombreTitularCta;
    String numTarjeta;
    String codInstitucion;
    String tipoCuenta;
    String numCuenta;
    String correoElectronico;
    String numCelular;
    String estadoGuardarBenefici;
    String tipoTarjeta;

    public String getTipIdentiCta() {
        return tipIdentiCta;
    }

    public void setTipIdentiCta(String tipIdentiCta) {
        this.tipIdentiCta = tipIdentiCta;
    }

    public String getNumIdentifiacion() {
        return numIdentifiacion;
    }

    public void setNumIdentifiacion(String numIdentifiacion) {
        this.numIdentifiacion = numIdentifiacion;
    }

    public String getNombreTitularCta() {
        return nombreTitularCta;
    }

    public void setNombreTitularCta(String nombreTitularCta) {
        this.nombreTitularCta = nombreTitularCta;
    }

    public String getCodInstitucion() {
        return codInstitucion;
    }

    public void setCodInstitucion(String codInstitucion) {
        this.codInstitucion = codInstitucion;
    }

    public String getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(String tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public String getNumCuenta() {
        return numCuenta;
    }

    public void setNumCuenta(String numCuenta) {
        this.numCuenta = numCuenta;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public String getNumCelular() {
        return numCelular;
    }

    public void setNumCelular(String numCelular) {
        this.numCelular = numCelular;
    }

    public String getEstadoGuardarBenefici() {
        return estadoGuardarBenefici;
    }

    public void setEstadoGuardarBenefici(String estadoGuardarBenefici) {
        this.estadoGuardarBenefici = estadoGuardarBenefici;
    }
    public String getNumTarjeta() {
        return numTarjeta;
    }

    public void setNumTarjeta(String numTarjeta) {
        this.numTarjeta = numTarjeta;
    }

    public String getTipoTarjeta() {
        return tipoTarjeta;
    }

    public void setTipoTarjeta(String tipoTarjeta) {
        this.tipoTarjeta = tipoTarjeta;
    }
}
