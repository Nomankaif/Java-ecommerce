package com.example.UserSignupJwtRole.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    private final SecretKey key = Keys.hmacShaKeyFor(
            "local-demo-only-jwt-signing-secret-key-123456789"
                    .getBytes(StandardCharsets.UTF_8));

    @Test
    void generatedTokenIsSignedAndValidForFifteenMinutes() {
        String token = jwtService.generateToken("user@example.com");
        String[] parts = token.split("\\.", -1);
        assertEquals(3, parts.length);
        assertFalse(parts[2].isEmpty());

        var signed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        assertEquals("HS256", signed.getHeader().getAlgorithm());
        assertEquals(900_000L, signed.getPayload().getExpiration().getTime()
                - signed.getPayload().getIssuedAt().getTime());
        assertTrue(signed.getPayload().getExpiration().after(new Date()));
        assertEquals("user@example.com", jwtService.validateAndGetEmail(token));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = Jwts.builder()
                .subject("user@example.com")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.validateAndGetEmail(token));
    }

    @Test
    void tamperedSignatureIsRejected() {
        String token = jwtService.generateToken("user@example.com");
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart)
                + replacement + token.substring(signatureStart + 1);

        assertThrows(SignatureException.class, () -> jwtService.validateAndGetEmail(tampered));
    }

    @Test
    void unsignedTokenIsRejected() {
        String token = Jwts.builder()
                .subject("user@example.com")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .compact();

        assertThrows(JwtException.class, () -> jwtService.validateAndGetEmail(token));
    }
}
