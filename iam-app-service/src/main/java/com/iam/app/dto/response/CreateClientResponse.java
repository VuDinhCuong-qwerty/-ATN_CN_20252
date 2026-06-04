package com.iam.app.dto.response;

import com.iam.app.domain.AuthClient;
import lombok.Getter;

@Getter
public class CreateClientResponse {

    private final Long id;
    private final String clientId;
    private final String name;
    private final String clientType;
    private final String clientSecret;

    public CreateClientResponse(AuthClient client, String rawSecret) {
        this.id = client.getId();
        this.clientId = client.getClientId();
        this.name = client.getName();
        this.clientType = client.getClientType();
        this.clientSecret = rawSecret;
    }
}
