package com.iam.jobScheduled.config;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.iam.jobScheduled.connect.RegionClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RestClientConfig {

    @Value("${region.url}")
    private String regionUrl;

    @Bean
    public RegionClient regionClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS) // 🔥 quan trọng
            .build();

        RestClient restClient = RestClient.builder()
                .baseUrl(regionUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .requestInterceptor((request, body, execution) -> {

                    log.info("➡️ Calling API: {}", request.getURI().toString());
                    var response = execution.execute(request, body);
                    log.info("⬅️ Response status: {}", response.getStatusCode());

                    return response;
                })
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(adapter)
                .build();

        return factory.createClient(RegionClient.class);
    }
}
