package com.iam.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_RESOURCE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_resource")
    @SequenceGenerator(name = "seq_auth_resource", sequenceName = "SEQ_AUTH_RESOURCE", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "APP_ID")
    private Long appId;

    @Column(name = "RESOURCE_CODE")
    private String resourceCode;

    @Column(name = "RESOURCE_NAME")
    private String resourceName;

    @Column(name = "RESOURCE_TYPE")
    private String resourceType;

    @Column(name = "ACTIONS")
    private String actions;

    @Column(name = "LDAP_GROUP_NAME")
    private String ldapGroupName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (status == null)
            status = "ACTIVE";
        createdAt = LocalDateTime.now();
    }
}
