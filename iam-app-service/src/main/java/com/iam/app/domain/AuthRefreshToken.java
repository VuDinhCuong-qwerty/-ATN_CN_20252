package com.iam.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AUTH_REFRESH_TOKEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRefreshToken {

    @Id
    @Column(name = "TOKEN")
    private String token;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "CLIENT_ID")
    private String clientId;

    @Column(name = "APP_ID")
    private Long appId;

    @Column(name = "USER_SESSION_ID")
    private String userSessionId;

    @Column(name = "SCOPES")
    private String scopes;

    @Column(name = "STATUS") // e.g. ACTIVE, REVOKED, EXPIRED
    private String status;

    @Column(name = "CREATED_AT")
    private Long createdAt;

    @Column(name = "EXPIRES_AT")
    private Long expiresAt;

}
