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

import org.springframework.stereotype.Component;

@Component
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

        return Jwts.builder()
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
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] resolveSecret(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
