package com.iam.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_APPLICATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_application")
    @SequenceGenerator(name = "seq_auth_application", sequenceName = "SEQ_AUTH_APPLICATION", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    /** INTERNAL | THIRD_PARTY_LDAP */
    @Column(name = "APP_TYPE")
    private String appType;

    @Column(name = "LOGO_URI")
    private String logoUri;

    @Column(name = "DEFAULT_URL")
    private String defaultUrl;

    /** ACTIVE | INACTIVE */
    @Column(name = "STATUS")
    private String status;

    @Column(name = "DEPARTMENT_ID")
    private Long departmentId;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "SERVICE_CODE")
    private String serviceCode;

    @Column(name = "GROUP_ID")
    private Long groupId;

    @Column(name = "ACR_LEVEL")
    private Long acrLevel;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "ACTIVE";
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public interface STATUS {
        String ACTIVE = "ACTIVE";
        String INACTIVE = "INACTIVE";
    }
}
