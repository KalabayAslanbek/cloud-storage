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

/**
 * Service responsible for issuing and validating JWT access tokens.
 *
 * Tokens:
 * - subject: username
 * - issuer: configured via {@code jwt.issuer}
 * - expiration: configured via {@code jwt.expMinutes}
 *
 * Secret handling:
 * - accepts either Base64-encoded secret (common in deployments) or plain UTF-8 string
 * - requires at least 32 bytes for HMAC-SHA signing key strength
 */
@Service
public class JwtService {

    private final byte[] secretKey;
    private final long expMinutes;
    private final String issuer;

    /**
     * Creates JWT service with configured secret, expiration and issuer.
     *
     * @param secret JWT signing secret. May be Base64-encoded or plain text. Must not be blank.
     * @param expMinutes token lifetime in minutes (default: 60)
     * @param issuer token issuer claim (default: cloud-storage)
     * @throws IllegalStateException if secret is missing/blank or shorter than 32 bytes
     */
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

    /**
     * Generates a signed JWT access token for the given username.
     *
     * @param username username to be stored in token subject claim
     * @return compact JWT string
     */
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

    /**
     * Validates the token signature and extracts username from the token subject.
     *
     * @param token compact JWT string (without "Bearer " prefix)
     * @return username stored in the token subject claim
     * @throws io.jsonwebtoken.ExpiredJwtException if token is expired
     * @throws io.jsonwebtoken.JwtException if token is invalid or cannot be parsed
     */
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

    /**
     * Returns configured token lifetime in seconds.
     *
     * @return token expiration duration in seconds
     */
    public long getExpiresInSeconds() {
        return expMinutes * 60;
    }
}