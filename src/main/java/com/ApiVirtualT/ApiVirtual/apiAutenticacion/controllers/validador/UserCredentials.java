package com.ApiVirtualT.ApiVirtual.apiAutenticacion.controllers.validador;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCredentials {
    private String CliacUsuVirtu;

    public String getCliacUsuVirtu() {
        return CliacUsuVirtu;
    }

    public String getClienWwwPswrd() {
        return ClienWwwPswrd;
    }

    private String ClienWwwPswrd;

    public void setClienWwwPswrd(String clienWwwPswrd) {
        ClienWwwPswrd = clienWwwPswrd;
    }

    public void setCliacUsuVirtu(String cliacUsuVirtu) {
        CliacUsuVirtu = cliacUsuVirtu;
    }
}
