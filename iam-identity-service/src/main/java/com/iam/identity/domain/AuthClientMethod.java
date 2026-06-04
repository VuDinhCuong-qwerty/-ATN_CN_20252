package com.iam.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_CLIENT_METHOD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthClientMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_client_method")
    @SequenceGenerator(name = "seq_auth_client_method", sequenceName = "SEQ_AUTH_CLIENT_METHOD", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "APP_ID")
    private Long appId;

    @Column(name = "METHOD_ID")
    private Long methodId;

    @Column(name = "CONFIG")
    private String config;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
