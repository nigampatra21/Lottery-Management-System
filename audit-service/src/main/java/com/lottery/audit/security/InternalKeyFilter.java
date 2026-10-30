package com.lottery.audit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    private final byte[] expected;

    public InternalKeyFilter(@Value("${audit.internal-key}") String key) {
        if (key == null || key.length() < 32) {
            throw new IllegalStateException(
                    "audit.internal-key must be at least 32 characters");
        }
        this.expected = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String presented = request.getHeader("X-Internal-Key");
        // Constant-time comparison, so the key can't be guessed from response timing
        if (presented != null
                && MessageDigest.isEqual(
                        presented.getBytes(StandardCharsets.UTF_8), expected)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "internal-service", null,
                            List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))));
        }
        chain.doFilter(request, response);
    }
}