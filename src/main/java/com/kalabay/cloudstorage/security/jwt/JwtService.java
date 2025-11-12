package com.kalabay.cloudstorage.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final byte[] secretKey;
    private final long expMinutes;
    private final String issuer;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expMinutes:60}") long expMinutes, @Value("${jwt.issuer:cloud-storage}") String issuer) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret must be set");
        }
        this.secretKey = (secret.matches("^[A-Za-z0-9+/=]+$") && secret.length() % 4 == 0) ? Decoders.BASE64.decode(secret) : secret.getBytes(StandardCharsets.UTF_8);
        if (this.secretKey.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes");
        }
        this.expMinutes = expMinutes;
        this.issuer = issuer;
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expMinutes))))
                .signWith(Keys.hmacShaKeyFor(secretKey))
                .compact();
    }

    public String validateAndGetUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
            throw new io.jsonwebtoken.ExpiredJwtException(null, claims, "Token expired");
        }
        return claims.getSubject(); 
    }

    public long getExpiresInSeconds() {
        return expMinutes * 60;
    }
}