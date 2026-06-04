package com.iam.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logs the outgoing request that gateway sends to a backend service.
 * Order = 3: runs AFTER UserContextFilter (1) and TokenExchangeFilter (2),
 * so headers already contain X-User-* context and the service Bearer token.
 */
@Slf4j
@Component
public class LogOutboundFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();

        // Build full backend URL: route URI + path + query
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String backendUrl;
        if (route != null) {
            String routeUri = route.getUri().toString().replaceAll("/$", "");
            String path = request.getURI().getRawPath();
            String query = request.getURI().getRawQuery();
            backendUrl = routeUri + path + (query != null ? "?" + query : "");
        } else {
            backendUrl = request.getURI().toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n[GATEWAY → BACKEND] ").append(request.getMethod()).append(" ").append(backendUrl);
        sb.append("\n  Headers :");
        request.getHeaders().forEach((name, values) ->
                sb.append("\n    ").append(name).append(": ").append(String.join(", ", values))
        );

        log.info(sb.toString());
        return chain.filter(exchange);
    }
}
