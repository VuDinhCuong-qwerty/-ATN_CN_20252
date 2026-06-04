package com.demo.change.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.demo.change.feign.IdentityClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${identity.service.uri}")
    private String identityServiceUri;

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService clientService) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clientService);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    public WebClient identityWebClient(OAuth2AuthorizedClientManager manager) {
        ExchangeFilterFunction tokenFilter = ExchangeFilterFunction.ofRequestProcessor(req -> {
            OAuth2AuthorizeRequest authReq = OAuth2AuthorizeRequest
                    .withClientRegistrationId("iam-service")
                    .principal("change-app-service")
                    .build();
            OAuth2AuthorizedClient authorized = manager.authorize(authReq);
            if (authorized == null) {
                log.error("Failed to obtain service token from IAM for client 'iam-service'");
                return Mono.error(new IllegalStateException("Could not obtain service token"));
            }
            log.debug("Service token obtained, expires at: {}", authorized.getAccessToken().getExpiresAt());
            return Mono.just(ClientRequest.from(req)
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + authorized.getAccessToken().getTokenValue())
                    .build());
        });

        return WebClient.builder()
                .baseUrl(identityServiceUri)
                .filter(tokenFilter)
                .build();
    }

    @Bean
    public IdentityClient identityClient(WebClient identityWebClient) {
        return HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(identityWebClient))
                .build()
                .createClient(IdentityClient.class);
    }
}
