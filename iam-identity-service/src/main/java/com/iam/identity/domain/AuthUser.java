package com.iam.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_USER")
@org.hibernate.annotations.DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_user")
    @SequenceGenerator(name = "seq_auth_user", sequenceName = "SEQ_AUTH_USER", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    // active || inactive || deleted
    @Column(name = "STATUS")
    private String status;

    @Column(name = "MS365_SYNCED")
    private Integer ms365Synced;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "MOBILE")
    private String mobile;

    @Column(name = "FORCE_CHANGE_PASSWORD")
    private Integer forceChangePassword;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "ACTIVE";
        if (ms365Synced == null) ms365Synced = 0;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
