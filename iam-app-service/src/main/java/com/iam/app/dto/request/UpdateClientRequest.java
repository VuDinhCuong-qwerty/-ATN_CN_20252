package com.iam.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class UpdateClientRequest {

    @NotBlank(message = "name không được để trống")
    private String name;

    private String logoUri;
    private String description;
    private String defaultUrl;

    private Boolean enabled;

    private List<String> grantTypes;

    private String redirectUris;

    private Integer accessTokenTtl;
    private Integer refreshTokenTtl;
    private Integer idTokenTtl;

    private Boolean requirePkce;
    // private Boolean requireConsent;
    private String postLogoutRedirect;

    private String tokenEndpointAuth;
}
