package com.iam.app.dto.response;

import com.iam.app.domain.AuthApplication;
import com.iam.app.domain.AuthClient;
import com.iam.app.domain.AuthClientOauth;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ClientDetailResponse {

    private final Long id;
    private final String clientId;
    private final String name;
    private final String clientType;
    private final Integer enabled;
    private final String logoUri;
    private final String description;
    private final String defaultUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private final AppInfo app;

    private final String grantTypes;
    private final String redirectUris;
    private final String allowedScopes;
    private final Integer accessTokenTtl;
    private final Integer refreshTokenTtl;
    private final Integer idTokenTtl;
    private final String tokenEndpointAuth;
    private final Boolean requirePkce;
    private final Boolean requireConsent;
    private final String postLogoutRedirect;

    private final List<String> warnings;

    public ClientDetailResponse(AuthClient client, AuthClientOauth oauth, AuthApplication app) {
        this(client, oauth, app, null);
    }

    public ClientDetailResponse(AuthClient client, AuthClientOauth oauth, AuthApplication app,
                                List<String> warnings) {
        this.id = client.getId();
        this.clientId = client.getClientId();
        this.name = client.getName();
        this.clientType = client.getClientType();
        this.enabled = client.getEnabled();
        this.logoUri = client.getLogoUri();
        this.description = client.getDescription();
        this.defaultUrl = client.getDefaultUrl();
        this.createdAt = client.getCreatedAt();
        this.updatedAt = client.getUpdatedAt();
        this.app = app != null ? new AppInfo(app) : null;
        this.warnings = (warnings != null && !warnings.isEmpty()) ? warnings : null;

        if (oauth != null) {
            this.grantTypes = oauth.getGrantTypes();
            this.redirectUris = oauth.getRedirectUris();
            this.allowedScopes = oauth.getAllowedScopes();
            this.accessTokenTtl = oauth.getAccessTokenTtl();
            this.refreshTokenTtl = oauth.getRefreshTokenTtl();
            this.idTokenTtl = oauth.getIdTokenTtl();
            this.tokenEndpointAuth = oauth.getTokenEndpointAuth();
            this.requirePkce = oauth.getRequirePkce() != null && oauth.getRequirePkce() == 1;
            this.requireConsent = oauth.getRequireConsent() != null && oauth.getRequireConsent() == 1;
            this.postLogoutRedirect = oauth.getPostLogoutRedirect();
        } else {
            this.grantTypes = null;
            this.redirectUris = null;
            this.allowedScopes = null;
            this.accessTokenTtl = null;
            this.refreshTokenTtl = null;
            this.idTokenTtl = null;
            this.tokenEndpointAuth = null;
            this.requirePkce = null;
            this.requireConsent = null;
            this.postLogoutRedirect = null;
        }
    }

    @Getter
    public static class AppInfo {
        private final Long id;
        private final String name;
        private final String serviceCode;
        private final String status;

        public AppInfo(AuthApplication app) {
            this.id = app.getId();
            this.name = app.getName();
            this.serviceCode = app.getServiceCode();
            this.status = app.getStatus();
        }
    }
}
