package com.iam.notify.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Read-only mirror của AUTH_USER từ iam-identity-service (chỉ field cần thiết)
@Entity
@Table(name = "AUTH_USER")
@Getter
@NoArgsConstructor
public class AuthUser {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "STATUS")
    private String status;
}
