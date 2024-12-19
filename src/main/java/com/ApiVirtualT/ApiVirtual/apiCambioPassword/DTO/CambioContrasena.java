package com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CambioContrasena {
    public String PassActual;
    public String PassNew;
    public String ConfPassNew;

    public String getPassActual() {
        return PassActual;
    }

    public void setPassActual(String passActual) {
        PassActual = passActual;
    }

    public String getPassNew() {
        return PassNew;
    }

    public void setPassNew(String passNew) {
        PassNew = passNew;
    }

    public String getConfPassNew() {
        return ConfPassNew;
    }

    public void setConfPassNew(String confPassNew) {
        ConfPassNew = confPassNew;
    }
}
