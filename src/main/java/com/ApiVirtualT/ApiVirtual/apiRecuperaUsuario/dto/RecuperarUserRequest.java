package com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class RecuperarUserRequest {
    private String numIdentifcacion;
    private String fechaNacimiento;
    private Integer ctrEstado;
    private String dirEmail;

    public String getNumIdentifcacion() {
        return numIdentifcacion;
    }

    public void setNumIdentifcacion(String numIdentifcacion) {
        this.numIdentifcacion = numIdentifcacion;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }


    public String getDirEmail() {
        return dirEmail;
    }

    public void setDirEmail(String dirEmail) {
        this.dirEmail = dirEmail;
    }

    public Integer getCtrEstado() {
        return ctrEstado;
    }

    public void setCtrEstado(Integer ctrEstado) {
        this.ctrEstado = ctrEstado;
    }
}
