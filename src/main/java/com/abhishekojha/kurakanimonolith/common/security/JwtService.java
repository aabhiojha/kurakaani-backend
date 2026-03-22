package com.abhishekojha.kurakanimonolith.common.security;

import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(jwtProperties.getSecret()));
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getExpirationSeconds());

        List<String> roles = user.getRoles()
                .stream()
                .map(role -> role.name())
                .sorted()
                .toList();

        String token = Jwts.builder()
                .setSubject(user.getEmail())
                .addClaims(Map.of(
                        "userId", user.getId(),
                        "name", user.getName(),
                        "roles", roles
                ))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
        log.debug("Generated JWT for user={}, userId={}, expiresAt={}", user.getEmail(), user.getId(), expiresAt);
        return token;
    }

    public Jws<Claims> parseToken(String token) {
        Jws<Claims> claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
        log.debug(
                "Parsed JWT successfully: subject={}, expiration={}",
                claims.getBody().getSubject(),
                claims.getBody().getExpiration()
        );
        return claims;
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            log.debug("JWT validation succeeded");
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    private byte[] resolveSecret(String secret) {
        try {
            byte[] decodedSecret = Decoders.BASE64.decode(secret);
            log.debug("Loaded JWT signing secret from base64 configuration");
            return decodedSecret;
        } catch (IllegalArgumentException ex) {
            log.debug("JWT secret is not base64 encoded, using raw UTF-8 bytes");
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
