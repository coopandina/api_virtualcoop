package com.ApiVirtualT.ApiVirtual.apiRegistroVirtual.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrearUsuario {
    public String Usuario;
    public String ConfirmUsuario;
    public String Password;
    public String ConfirPassword;
    public String PregunSeguridad;
    public String RespPregunSeguridad;
    public int CliacTermCondic;

    public String getUsuario() {
        return Usuario;
    }

    public void setUsuario(String usuario) {
        Usuario = usuario;
    }

    public String getConfirmUsuario() {
        return ConfirmUsuario;
    }

    public void setConfirmUsuario(String confirmUsuario) {
        ConfirmUsuario = confirmUsuario;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getConfirPassword() {
        return ConfirPassword;
    }

    public void setConfirPassword(String confirPassword) {
        ConfirPassword = confirPassword;
    }

    public String getPregunSeguridad() {
        return PregunSeguridad;
    }

    public void setPregunSeguridad(String pregunSeguridad) {
        PregunSeguridad = pregunSeguridad;
    }

    public String getRespPregunSeguridad() {
        return RespPregunSeguridad;
    }

    public void setRespPregunSeguridad(String respPregunSeguridad) {
        RespPregunSeguridad = respPregunSeguridad;
    }

    public int getCliacTermCondic() {
        return CliacTermCondic;
    }

    public void setCliacTermCondic(int cliacTermCondic) {
        CliacTermCondic = cliacTermCondic;
    }
}
