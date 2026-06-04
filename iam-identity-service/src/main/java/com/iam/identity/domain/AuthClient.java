package com.iam.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_CLIENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthClient {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_client")
    @SequenceGenerator(name = "seq_auth_client", sequenceName = "SEQ_AUTH_CLIENT", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CLIENT_ID")
    private String clientId;

    @Column(name = "CLIENT_SECRET")
    private String clientSecret;

    @Column(name = "NAME")
    private String name;

    @Column(name = "CLIENT_TYPE")
    private String clientType;

    @Column(name = "DEFAULT_URL")
    private String defaultUrl;

    @Column(name = "ENABLED")
    private Integer enabled;

    @Column(name = "APP_ID")
    private Long appId;

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

    public boolean isEnabled() {
        return this.enabled != null && this.enabled == 1;
    }
}