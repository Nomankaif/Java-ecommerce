package com.example.UserSignupJwtRole.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Service
public class JwtService {


    private static final String SECRET="local-demo-only-jwt-signing-secret-key-123456789";

    private static final SecretKey KEY= Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static final long EXPIRATION_MS = 15 * 60 * 1000L;


    public String generateToken(String email){

        long now=System.currentTimeMillis();

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    public String validateAndGetEmail(String token){
        Claims claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getExpiration() == null) {
            throw new JwtException("Token has no expiration");
        }

        String email = claims.getSubject();

        if (email == null || email.isBlank()) {
            throw new JwtException("Token has no subject");
        }

        return email;
    }
}
