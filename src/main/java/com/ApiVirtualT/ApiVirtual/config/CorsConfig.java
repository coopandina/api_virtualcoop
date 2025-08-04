package com.ApiVirtualT.ApiVirtual.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica CORS a todos los endpoints
                .allowedOrigins(
                        "http://localhost:9090",
                        "http://172.1.0.204:9090","http://localhost:4173", "http://localhost:8989", "http://pruebasvirtual.coopandina.fin.ec:8989",
                        "http://betavirtualcoop.coopandina.fin.ec:8080",
                        "http://172.1.0.136:8989",
                        "https://virtualcoop.coopandina.fin.ec:80",
                        "https://virtualcoop.coopandina.fin.ec",
                        "https://virtualcoop.coopandina.fin.ec:8080",
                        "https://andinadigital.coopandina.fin.ec",
                        "https://andinadigital.coopandina.fin.ec:80",
                        "https://andinadigital.coopandina.fin.ec:8080",
                        "https://andinadigital.coopandina.fin.ec:4173",
                        "http://localhost:1110"

                ) // Orígenes permitidos
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Métodos HTTP permitidos
                .allowedHeaders("*") // Headers permitidos
                .allowCredentials(true); // Permitir envío de cookies/sesión
    }
}
