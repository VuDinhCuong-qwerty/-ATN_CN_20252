package com.iam.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_DEPARTMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_department")
    @SequenceGenerator(name = "seq_auth_department", sequenceName = "SEQ_AUTH_DEPARTMENT", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE")
    private String code;

    @Column(name = "NAME")
    private String name;

    @Column(name = "PARENT_ID")
    private Long parentId;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
