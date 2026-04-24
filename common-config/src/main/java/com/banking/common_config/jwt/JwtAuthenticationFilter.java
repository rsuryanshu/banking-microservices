package com.banking.common_config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@ConditionalOnClass(name = "org.springframework.security.web.SecurityFilterChain")
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private static final String AUTHORIZATION_KEY = "Authorization";
    private static final List<String> excludedPaths = Arrays.asList(
            "/auth",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/accounts/v3/api-docs",
            "/transactions/v3/api-docs",
            "/notifications/v3/api-docs",
            "/users/v3/api-docs",
            "/webjars"
    );


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestURI = request.getRequestURI();
        boolean shouldSkip = excludedPaths.stream().anyMatch(requestURI::startsWith);
        if (shouldSkip) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_KEY);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or not a Bearer token");
            response.getWriter().write("Authorization header missing or not a Bearer token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            log.warn("Bearer token is empty");
            response.getWriter().write("Bearer token is empty");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            String username = jwtUtil.extractUsername(token);
            List<String> roles = jwtUtil.extractRole(token);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtUtil.isTokenValid(token)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Jwt Authentication failed: {}", e.getMessage());
            response.getWriter().write("Jwt Authentication failed");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}