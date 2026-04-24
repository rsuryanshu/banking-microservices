package com.banking.api_gateway.filter;

import com.banking.common_config.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    private static final List<String> OPEN_PATHS = List.of(
            "/users/auth/login",
            "/users/auth/register",
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
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        boolean isOpen = OPEN_PATHS.stream().anyMatch(path::startsWith);
        if (isOpen) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return writeErrorResponse(exchange, "Authorization header missing or invalid");
        }

        String token = authHeader.substring(7).trim();

        try {
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid token for path: {}", path);
                return writeErrorResponse(exchange, "Invalid or expired token");
            }

            return chain.filter(exchange);

        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return writeErrorResponse(exchange, "Jwt validation Failed");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> error = Map.of(
                "timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "status", 401,
                "error", "Unauthorized",
                "message", message
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
