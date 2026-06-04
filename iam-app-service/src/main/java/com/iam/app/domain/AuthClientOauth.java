package com.iam.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_CLIENT_OAUTH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthClientOauth {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_client_oauth")
    @SequenceGenerator(name = "seq_auth_client_oauth", sequenceName = "SEQ_AUTH_CLIENT_OAUTH", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CLIENT_ID")
    private Long clientId;

    @Column(name = "GRANT_TYPES")
    private String grantTypes;

    @Column(name = "REDIRECT_URIS")
    private String redirectUris;

    @Column(name = "ALLOWED_SCOPES")
    private String allowedScopes;

    @Column(name = "ACCESS_TOKEN_TTL")
    private Integer accessTokenTtl;

    @Column(name = "REFRESH_TOKEN_TTL")
    private Integer refreshTokenTtl;

    @Column(name = "ID_TOKEN_TTL")
    private Integer idTokenTtl;

    @Column(name = "TOKEN_ENDPOINT_AUTH")
    private String tokenEndpointAuth;

    @Column(name = "REQUIRE_PKCE")
    private Integer requirePkce;

    @Column(name = "REQUIRE_CONSENT")
    private Integer requireConsent;

    @Column(name = "POST_LOGOUT_REDIRECT")
    private String postLogoutRedirect;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
