package com.ApiVirtualT.ApiVirtual.apiDashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class verMovimientoCta {
    String ctadp_cod_ctadp;
    String fechaDesdeCons;
    String fechaHastaCons;
    String nombreApellidosBus;
    String correoBeneficiario;
    String estadoGuardarBenefici;


    public String getCorreoBeneficiario() {
        return correoBeneficiario;
    }

    public void setCorreoBeneficiario(String correoBeneficiario) {
        this.correoBeneficiario = correoBeneficiario;
    }

    public String getEstadoGuardarBenefici() {
        return estadoGuardarBenefici;
    }

    public void setEstadoGuardarBenefici(String estadoGuardarBenefici) {
        this.estadoGuardarBenefici = estadoGuardarBenefici;
    }

    public String getNombreApellidosBus() {
        return nombreApellidosBus;
    }

    public void setNombreApellidosBus(String nombreApellidosBus) {
        this.nombreApellidosBus = nombreApellidosBus;
    }

    public String getFechaDesdeCons() {
        return fechaDesdeCons;
    }

    public void setFechaDesdeCons(String fechaDesdeCons) {
        this.fechaDesdeCons = fechaDesdeCons;
    }

    public String getFechaHastaCons() {
        return fechaHastaCons;
    }

    public void setFechaHastaCons(String fechaHastaCons) {
        this.fechaHastaCons = fechaHastaCons;
    }

    public String getCtadp_cod_ctadp() {
        return ctadp_cod_ctadp;
    }

    public void setCtadp_cod_ctadp(String ctadp_cod_ctadp) {
        this.ctadp_cod_ctadp = ctadp_cod_ctadp;
    }

}
