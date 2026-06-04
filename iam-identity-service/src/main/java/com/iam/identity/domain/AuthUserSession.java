package com.iam.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_USER_SESSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserSession {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "ACR_LEVEL")
    private Long acrLevel;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "LAST_ACCESS")
    private LocalDateTime lastAccess;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @Column(name = "USER_AGENT")
    private String userAgent;
}