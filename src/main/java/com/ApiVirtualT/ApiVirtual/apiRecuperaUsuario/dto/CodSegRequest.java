package com.ApiVirtualT.ApiVirtual.apiRecuperaUsuario.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodSegRequest {

    private String Codaccess_codigo_temporal;

    public String getCodaccess_codigo_temporal() {
        return Codaccess_codigo_temporal;
    }

    public void setCodaccess_codigo_temporal(String codaccess_codigo_temporal) {
        Codaccess_codigo_temporal = codaccess_codigo_temporal;
    }
}
