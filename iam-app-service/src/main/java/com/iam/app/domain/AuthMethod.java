package com.iam.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_METHOD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_method")
    @SequenceGenerator(name = "seq_auth_method", sequenceName = "SEQ_AUTH_METHOD", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "METHOD")
    private String method;

    @Column(name = "STATUS")
    private Integer status;

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

    public interface METHOD {
        String USERNAME_PASSWORD = "USERNAME_PASSWORD";
        String OTP_EMAIL = "OTP_EMAIL";
        String TOTP = "TOTP";
    }
}