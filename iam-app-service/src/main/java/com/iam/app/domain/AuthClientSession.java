package com.iam.app.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_CLIENT_SESSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthClientSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_client_session")
    @SequenceGenerator(name = "seq_auth_client_session", sequenceName = "SEQ_CLIENT_SESSION_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_SESSION_ID")
    private String sessionId;

    @Column(name = "APP_ID")
    private Long appId;

    @Column(name = "STATUS")
    private Long status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiredAt;
}
