package com.iam.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class PermissionCheckFilterFactory extends AbstractGatewayFilterFactory<PermissionCheckFilterFactory.Config> {

    private final ObjectMapper objectMapper;

    public PermissionCheckFilterFactory(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    List<String> perms = auth.getToken().getClaimAsStringList("permissions");
                    // Each entry in permissions is one action: "iam-service/user:read"
                    // contains() is sufficient — no parsing needed
                    if (perms != null && perms.contains(config.getPermission())) {
                        return chain.filter(exchange);
                    }
                    return writeForbidden(exchange);
                });
    }

    private Mono<Void> writeForbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "status", 403,
                "errorCode", "FORBIDDEN",
                "errorDesc", "Không có quyền thực hiện thao tác này",
                "timestamp", Instant.now().toString(),
                "path", exchange.getRequest().getPath().value()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"status\":403,\"errorCode\":\"FORBIDDEN\"}".getBytes();
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Config {
        private String permission;
    }
}
