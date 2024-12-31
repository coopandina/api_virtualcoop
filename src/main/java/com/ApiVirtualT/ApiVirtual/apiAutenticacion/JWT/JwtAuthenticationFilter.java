package com.ApiVirtualT.ApiVirtual.apiAutenticacion.JWT;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Key key = Keys.hmacShaKeyFor("TuClaveSecretaSuperSeguraParaLaApiVirtualT2024".getBytes()); // Usa la misma clave que generaste


    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String subject = claims.getSubject();
            if (subject != null) {
                String[] parts = subject.split(",");

                // Validar según el número de partes del token
                if (parts.length == 3) {
                    // Token de usuario completo
                    String CliacUsuVirtu = parts[0];
                    String ClienIdenti = parts[1];
                    String numSocio = parts[2];

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(CliacUsuVirtu, null, new ArrayList<>());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    request.setAttribute("CliacUsuVirtu", CliacUsuVirtu);
                    request.setAttribute("ClienIdenti", ClienIdenti);
                    request.setAttribute("numSocio", numSocio);
                }
                else if (parts.length == 2) {
                    // Token de registro
                    String ClienIdenti = parts[0];
                    String fecNacClien = parts[1];

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(ClienIdenti, null, new ArrayList<>());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    request.setAttribute("ClienIdenti", ClienIdenti);
                    request.setAttribute("fecNacClien", fecNacClien);
                    request.setAttribute("tokenType", "registro");
                }
                else {
                    throw new JwtException("Formato de token no válido");
                }
            }

        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Token inválido o expirado");
            errorResponse.put("message", e.getMessage());

            String jsonResponse = new ObjectMapper().writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            return;
        }

        filterChain.doFilter(request, response);
    }

}

