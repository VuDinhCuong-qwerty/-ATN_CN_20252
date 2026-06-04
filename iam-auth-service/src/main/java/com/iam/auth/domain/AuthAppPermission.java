package com.iam.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_APP_PERMISSION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthAppPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_app_permission")
    @SequenceGenerator(name = "seq_auth_app_permission", sequenceName = "SEQ_AUTH_APP_PERMISSION", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "APP_ID", nullable = false)
    private Long appId;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "GRANTED_BY")
    private String grantedBy;

    @Column(name = "GRANTED_AT")
    private LocalDateTime grantedAt;

    @Column(name = "EXPIRED_AT")
    private LocalDateTime expiredAt;

    @Column(name = "REVOKED_BY")
    private String revokedBy;

    @Column(name = "REVOKED_AT")
    private LocalDateTime revokedAt;

    @Column(name = "GRANT_SOURCE")
    private String grantSource;

    @Column(name = "REQUEST_ID")
    private Long requestId;

}