package com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class DatosRegistro {
    public String ClienIdeClien;
    public String FechaNacimiento;
    public String ClaveControl;

    public String getClienIdeClien() {
        return ClienIdeClien;
    }

    public void setClienIdeClien(String clienIdeClien) {
        ClienIdeClien = clienIdeClien;
    }

    public String getFechaNacimiento() {
        return FechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        FechaNacimiento = fechaNacimiento;
    }

    public String getClaveControl() {
        return ClaveControl;
    }

    public void setClaveControl(String claveControl) {
        ClaveControl = claveControl;
    }
}
