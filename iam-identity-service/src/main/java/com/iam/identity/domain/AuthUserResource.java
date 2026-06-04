package com.iam.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_USER_RESOURCE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_user_resource")
    @SequenceGenerator(name = "seq_auth_user_resource", sequenceName = "SEQ_AUTH_USER_RESOURCE", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "RESOURCE_ID")
    private Long resourceId;

    @Column(name = "ACTION")
    private String action;

    @Column(name = "STATUS")
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

    @Column(name = "REQUEST_ID")
    private Long requestId;

    @Column(name = "GRANT_SOURCE")
    private String grantSource;

    @Column(name = "INACTIVE_FROM_DATE")
    private LocalDateTime inactiveFromDate;

    @Column(name = "INACTIVE_TO_DATE")
    private LocalDateTime inactiveToDate;

    @PrePersist
    protected void onCreate() {
        if (status == null)
            status = "ACTIVE";
        grantedAt = LocalDateTime.now();
    }
}
