package com.ApiVirtualT.ApiVirtual.apiDashboard.controllers;


import com.ApiVirtualT.ApiVirtual.apiDashboard.services.UtilsTransService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    @Autowired
    private UtilsTransService dashservice;

    /**
     * Endpoint para dashboard
     */
    @PostMapping("/infouser")
    public ResponseEntity<Map<String, Object>> informacionUsuario(HttpServletRequest token){
        return dashservice.inforUsuarios(token);
    }




}
