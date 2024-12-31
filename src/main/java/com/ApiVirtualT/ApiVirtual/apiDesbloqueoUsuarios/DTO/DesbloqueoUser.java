package com.ApiVirtualT.ApiVirtual.apiDesbloqueoUsuarios.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DesbloqueoUser {
    public String CliacUsuVirtu;
    public String ClienIdeClien;
    public String ClienCodClien;
    public String FechaNacimiento;
    public String TipoIdentificacion;


    public String getTipoIdentificacion() {
        return TipoIdentificacion;
    }

    public void setTipoIdentificacion(String tipoIdentificacion) {
        TipoIdentificacion = tipoIdentificacion;
    }

    public String getFechaNacimiento() {
        return FechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        FechaNacimiento = fechaNacimiento;
    }

    public String getCliacUsuVirtu() {
        return CliacUsuVirtu;
    }

    public void setCliacUsuVirtu(String cliacUsuVirtu) {
        CliacUsuVirtu = cliacUsuVirtu;
    }

    public String getClienIdeClien() {
        return ClienIdeClien;
    }

    public void setClienIdeClien(String clienIdeClien) {
        ClienIdeClien = clienIdeClien;
    }

    public String getClienCodClien() {
        return ClienCodClien;
    }

    public void setClienCodClien(String clienCodClien) {
        ClienCodClien = clienCodClien;
    }
}
