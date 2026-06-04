package com.iam.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_SIGNING_KEY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSigningKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_signing_key")
    @SequenceGenerator(name = "seq_auth_signing_key", sequenceName = "SEQ_AUTH_SIGNING_KEY", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "KID")
    private String kid;

    @Column(name = "ALGORITHM")
    private String algorithm;

    @Lob
    @Column(name = "PUBLIC_KEY")
    private String publicKey;

    @Lob
    @Column(name = "PRIVATE_KEY")
    private String privateKey;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "VALID_FROM")
    private LocalDateTime validFrom;

    @Column(name = "VALID_UNTIL")
    private LocalDateTime validUntil;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        validFrom = LocalDateTime.now();
    }

    public enum Status {
        ACTIVE, PASSIVE, DISABLED
    }

    public enum Algorithm {
        RS256, ES256
    }
}