package com.iam.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts user claims from the validated JWT and injects them as X-headers
 * so backend services can identify the caller without re-parsing the token.
 *
 * Order = 1: runs AFTER PermissionCheckFilter (per-route, order=0),
 *            runs BEFORE TokenExchangeFilter (order=2).
 */
@Component
public class UserContextFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    var jwt = auth.getToken();
                    var mutated = exchange.getRequest().mutate()
                            .header("X-User-Id",       nullSafe(jwt.getSubject()))
                            .header("X-Employee-Code", nullSafe(jwt.getClaimAsString("employeeCode")))
                            .header("X-Username",      nullSafe(jwt.getClaimAsString("username")))
                            .header("X-User-Role",     nullSafe(jwt.getClaimAsString("role")))
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
