package com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor

public class CodSegurdiad {

    private String Codaccess_codigo_temporal;

    public String getCodaccess_codigo_temporal() {
        return Codaccess_codigo_temporal;
    }

    public void setCodaccess_codigo_temporal(String codaccess_codigo_temporal) {
        Codaccess_codigo_temporal = codaccess_codigo_temporal;
    }

    @Override
    public String toString() {
        return "CodSegurdiad{" +
                "Codaccess_codigo_temporal='" + Codaccess_codigo_temporal + '\'' +
                '}';
    }
}
