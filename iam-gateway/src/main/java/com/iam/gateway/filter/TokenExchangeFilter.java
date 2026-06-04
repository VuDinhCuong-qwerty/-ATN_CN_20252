package com.iam.gateway.filter;

import com.iam.gateway.config.ServiceTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Replaces the user's Bearer token with a service token (client_credentials)
 * before forwarding to backend services.
 *
 * Backend services only validate that the token comes from a trusted client —
 * they do NOT need the user's permissions claim. User context is carried via X-headers
 * injected by UserContextFilter (order=1).
 *
 * Order = 2: runs LAST in the filter chain (after PermissionCheck and UserContext).
 */
@Component
@RequiredArgsConstructor
public class TokenExchangeFilter implements GlobalFilter, Ordered {

    private final ServiceTokenProvider serviceTokenProvider;

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return serviceTokenProvider.getToken()
                .flatMap(token -> {
                    var mutated = exchange.getRequest().mutate()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }
}
