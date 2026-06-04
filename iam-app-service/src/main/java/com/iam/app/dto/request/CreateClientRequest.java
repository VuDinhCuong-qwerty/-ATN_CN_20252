package com.iam.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateClientRequest {

    @NotBlank(message = "clientId không được để trống")
    private String clientId;

    @NotBlank(message = "name không được để trống")
    private String name;

    @NotBlank(message = "type không được để trống")
    private String type;

    @NotNull(message = "appId không được để trống")
    private Long appId;

    @NotEmpty(message = "grantTypes không được để trống")
    private List<String> grantTypes;

    private String logoUri;
    private String description;
    private String defaultUrl;

    private String redirectUris;
    private List<String> scopes;

    private Integer accessTokenTtl;
    private Integer refreshTokenTtl;
    private Integer idTokenTtl;

    private Boolean requirePkce;
    private Boolean requireConsent;
    private String postLogoutRedirect;

    private String tokenEndpointAuth;
}
