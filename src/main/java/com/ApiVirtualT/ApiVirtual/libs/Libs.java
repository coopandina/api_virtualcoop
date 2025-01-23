package com.ApiVirtualT.ApiVirtual.libs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

public class Libs {

    private final EntityManager entityManager;

    public Libs(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    public String obtenerFecha() throws Exception {
        List<Object[]> resultadoFecha = ejecutarConsultaFechaHora();
        return resultadoFecha.get(0)[0].toString().trim();
    }
    public String obtenerHora() throws Exception {
        List<Object[]> resultadoFecha = ejecutarConsultaFechaHora();
        return resultadoFecha.get(0)[1].toString().trim();
    }

    public String obtenerFechaYHora() throws Exception {
        List<Object[]> resultadoFecha = ejecutarConsultaFechaHora();
        return resultadoFecha.get(0)[2].toString().trim();
    }

    private List<Object[]> ejecutarConsultaFechaHora() throws Exception {
        String sqlFechaHora = "CALL cnxprc_fecha_hora()";
        Query queryFecha = entityManager.createNativeQuery(sqlFechaHora);
        List<Object[]> resultadoFecha = queryFecha.getResultList();

        if (resultadoFecha.isEmpty()) {
            throw new Exception("No se pudo obtener la fecha actual del sistema.");
        }

        return resultadoFecha;
    }
}
