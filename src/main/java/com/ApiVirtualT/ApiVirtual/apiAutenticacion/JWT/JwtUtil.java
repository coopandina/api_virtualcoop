package com.ApiVirtualT.ApiVirtual.apiAutenticacion.JWT;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Key key = Keys.hmacShaKeyFor("TuClaveSecretaSuperSeguraParaLaApiVirtualT2024".getBytes());
    //private static final long EXPIRATION_TIME = 1000 * 60 * 2; // 2 minutos
    private static final long EXPIRATION_TIME = 1000 * 60 * 30; // 30 minutos


    public static String generateToken(String CliacUsuVirtu, String ClienIdenti, String numSocio){
        String subject = CliacUsuVirtu + "," + ClienIdenti + "," + numSocio;  // Concatenar los valores
        return Jwts.builder()
                .setSubject(subject)  // Solo se establece una vez el subject
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }





}
