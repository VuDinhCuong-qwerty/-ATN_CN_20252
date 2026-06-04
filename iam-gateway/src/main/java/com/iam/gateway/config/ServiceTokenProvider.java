package com.iam.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class ServiceTokenProvider {

    private record TokenEntry(String token, Instant expiresAt) {}

    // Caffeine not used: it doesn't support per-entry TTL (TTL is fixed at cache build time).
    // AtomicReference allows us to store expires_in from the server response each time.
    private final AtomicReference<TokenEntry> tokenRef = new AtomicReference<>();

    private final WebClient tokenWebClient;
    private final GatewayProperties props;

    public Mono<String> getToken() {
        TokenEntry entry = tokenRef.get();
        if (entry != null
                && Instant.now().plusSeconds(props.getRefreshBufferSeconds()).isBefore(entry.expiresAt())) {
            return Mono.just(entry.token());
        }
        return fetchNewToken();
    }

    private Mono<String> fetchNewToken() {
        String credentials = Base64.getEncoder().encodeToString(
                (props.getClientId() + ":" + props.getClientSecret()).getBytes());

        return tokenWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .doOnNext(resp -> tokenRef.set(
                        new TokenEntry(resp.accessToken(),
                                Instant.now().plusSeconds(resp.expiresIn()))
                ))
                .map(TokenResponse::accessToken);
        // Race condition (two concurrent requests both find cache stale):
        // both fetch independently, last write wins. Both tokens are valid — idempotent, no lock needed.
    }

    private record TokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
            @com.fasterxml.jackson.annotation.JsonProperty("expires_in") long expiresIn,
            @com.fasterxml.jackson.annotation.JsonProperty("token_type") String tokenType
    ) {}
}
