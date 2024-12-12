package com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CambioContrasenaCredencial {
    private String CliacUsuVirtu;
    private String NewPassword;
    private String ConfirmPassword;

    public String getCliacUsuVirtu() {
        return CliacUsuVirtu;
    }

    public void setCliacUsuVirtu(String cliacUsuVirtu) {
        CliacUsuVirtu = cliacUsuVirtu;
    }

    public String getNewPassword() {
        return NewPassword;
    }

    public void setNewPassword(String newPassword) {
        NewPassword = newPassword;
    }

    public String getConfirmPassword() {
        return ConfirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        ConfirmPassword = confirmPassword;
    }
}
