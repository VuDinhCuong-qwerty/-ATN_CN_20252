package com.iam.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Logs the original request received from the UI (before any filter mutation).
 * Order = -1: runs first, before PermissionCheck, UserContextFilter, TokenExchangeFilter.
 * For POST/PUT: reads and caches the body so downstream filters can still consume it.
 */
@Slf4j
@Component
public class LogInboundFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        HttpMethod httpMethod = req.getMethod();
        StringBuilder sb = buildLog(req);

        if (HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod)) {
            return DataBufferUtils.join(req.getBody())
                    .flatMap(buf -> {
                        byte[] bytes = new byte[buf.readableByteCount()];
                        buf.read(bytes);
                        DataBufferUtils.release(buf);

                        sb.append("\n  Body    : ").append(
                                bytes.length > 0 ? new String(bytes, StandardCharsets.UTF_8) : "(empty)"
                        );
                        log.info(sb.toString());

                        Flux<DataBuffer> cachedBody = Flux.just(
                                exchange.getResponse().bufferFactory().wrap(bytes)
                        );
                        ServerHttpRequest mutatedReq = new ServerHttpRequestDecorator(req) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return cachedBody;
                            }
                        };
                        return chain.filter(exchange.mutate().request(mutatedReq).build());
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        sb.append("\n  Body    : (empty)");
                        log.info(sb.toString());
                        return chain.filter(exchange);
                    }));
        }

        log.info(sb.toString());
        return chain.filter(exchange);
    }

    private StringBuilder buildLog(ServerHttpRequest req) {
        String methodName = req.getMethod() != null ? req.getMethod().name() : "UNKNOWN";
        StringBuilder sb = new StringBuilder();
        sb.append("\n[-> GATEWAY] ").append(methodName).append(" ").append(req.getURI());

        if (!req.getQueryParams().isEmpty()) {
            sb.append("\n  Query   : ").append(req.getQueryParams());
        }

        sb.append("\n  Headers :");
        req.getHeaders().forEach((name, values) ->
                sb.append("\n    ").append(name).append(": ").append(String.join(", ", values))
        );
        return sb;
    }
}
