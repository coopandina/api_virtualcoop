package com.ApiVirtualT.ApiVirtual.apiCambioPassword.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class ValidacionDatos {
    public String ClienIdeClien;

    public String getClienIdeClien() {
        return ClienIdeClien;
    }

    public void setClienIdeClien(String clienIdeClien) {
        ClienIdeClien = clienIdeClien;
    }
}
