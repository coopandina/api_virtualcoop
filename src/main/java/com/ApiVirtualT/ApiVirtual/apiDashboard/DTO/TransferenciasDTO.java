package com.ApiVirtualT.ApiVirtual.apiDashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferenciasDTO {
    String ctaEnvio;
    String ctaDestino;
    String codTempTransDirec;
    String txtdettrnsf;
    Float valtrans;

    public Float getValtrans() {
        return valtrans;
    }

    public void setValtrans(Float valtrans) {
        this.valtrans = valtrans;
    }

    public String getTxtdettrnsf() {
        return txtdettrnsf;
    }

    public void setTxtdettrnsf(String txtdettrnsf) {
        this.txtdettrnsf = txtdettrnsf;
    }

    public String getCtaEnvio() {
        return ctaEnvio;
    }

    public void setCtaEnvio(String ctaEnvio) {
        this.ctaEnvio = ctaEnvio;
    }

    public String getCtaDestino() {
        return ctaDestino;
    }

    public void setCtaDestino(String ctaDestino) {
        this.ctaDestino = ctaDestino;
    }

    public String getCodTempTransDirec() {
        return codTempTransDirec;
    }

    public void setCodTempTransDirec(String codTempTransDirec) {
        this.codTempTransDirec = codTempTransDirec;
    }
}
