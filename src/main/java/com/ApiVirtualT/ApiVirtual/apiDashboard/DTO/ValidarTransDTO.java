package com.ApiVirtualT.ApiVirtual.apiDashboard.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class ValidarTransDTO {
    String ctaValidar;
    Double valTrans;

    public String getCtaValidar() {
        return ctaValidar;
    }

    public void setCtaValidar(String ctaValidar) {
        this.ctaValidar = ctaValidar;
    }

    public Double getValTrans() {
        return valTrans;
    }

    public void setValTrans(Double valTrans) {
        this.valTrans = valTrans;
    }
}
