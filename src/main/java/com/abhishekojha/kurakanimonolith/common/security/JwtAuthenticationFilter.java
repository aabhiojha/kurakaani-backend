package com.abhishekojha.kurakanimonolith.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("JWT filter start: method={}, path={}", request.getMethod(), request.getRequestURI());

        String token = resolveToken(request);
        Authentication existingAuthentication = SecurityContextHolder.getContext().getAuthentication();

        if (token == null) {
            log.debug("No bearer token found for path={}", request.getRequestURI());
        } else if (existingAuthentication != null) {
            log.debug(
                    "Security context already contains authentication: principal={}, authorities={}",
                    existingAuthentication.getName(),
                    existingAuthentication.getAuthorities()
            );
        } else if (jwtService.isTokenValid(token)) {
            Jws<Claims> claimsJws = jwtService.parseToken(token);
            Claims claims = claimsJws.getBody();
            Collection<? extends GrantedAuthority> authorities = extractAuthorities(claims);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),
                    token,
                    authorities
            );
            ((UsernamePasswordAuthenticationToken) authentication)
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug(
                    "Authenticated request via JWT: principal={}, authorities={}, path={}",
                    claims.getSubject(),
                    authorities,
                    request.getRequestURI()
            );
        } else {
            log.debug("JWT token validation failed for path={}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
        Authentication finalAuthentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug(
                "JWT filter end: path={}, responseStatus={}, principal={}",
                request.getRequestURI(),
                response.getStatus(),
                finalAuthentication != null ? finalAuthentication.getName() : "anonymous"
        );
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (!(rolesClaim instanceof List<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .map(obj -> String.valueOf(obj))
                .map(role -> new SimpleGrantedAuthority(role))
                .toList();
    }
}
