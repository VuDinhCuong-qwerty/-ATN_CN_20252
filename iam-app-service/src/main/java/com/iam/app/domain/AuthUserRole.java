package com.iam.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_USER_ROLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_user_role")
    @SequenceGenerator(name = "seq_auth_user_role", sequenceName = "SEQ_AUTH_USER_ROLE", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "GRANTED_BY")
    private String grantedBy;

    @Column(name = "GRANTED_AT", updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "EXPIRED_AT")
    private LocalDateTime expiredAt;

    /** ACTIVE | REVOKED */
    @Column(name = "STATUS")
    private String status;

    @PrePersist
    protected void onCreate() {
        if (status == null)
            status = "ACTIVE";
        grantedAt = LocalDateTime.now();
    }
}
