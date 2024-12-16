package com.ApiVirtualT.ApiVirtual.apiAutenticacion.JWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Key key = Keys.hmacShaKeyFor("TuClaveSecretaSuperSeguraParaLaApiVirtualT2024".getBytes()); // Usa la misma clave que generaste


    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return; // Si no hay token, continuar sin autenticar
        }

        String token = authHeader.substring(7); // Eliminar "Bearer "

        try {
            // Validar el token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Extraer el subject del token
            String subject = claims.getSubject(); // "Aclascano21,1750442145,25310"
            if (subject != null) {
                // Dividir el subject en sus partes
                String[] parts = subject.split(",");
                if (parts.length == 3) {
                    String CliacUsuVirtu = parts[0];  // Usuario (Aclascano21)
                    String ClienIdenti = parts[1];   // Número de cédula (1750442145)
                    String numSocio = parts[2];      // Número de socio (25310)

                    // Configurar el contexto de seguridad con el usuario
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(CliacUsuVirtu, null, new ArrayList<>());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Opcional: Guardar los valores en el request para usarlos en los controladores
                    request.setAttribute("CliacUsuVirtu", CliacUsuVirtu);
                    request.setAttribute("ClienIdenti", ClienIdenti);
                    request.setAttribute("numSocio", numSocio);
                } else {
                    throw new JwtException("El formato del token no es válido");
                }
            }

        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token inválido o expirado");
            return; // Detener la cadena de filtros si el token no es válido
        }

        filterChain.doFilter(request, response); // Continuar con el filtro
    }

}

