package com.iam.notify.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Read-only mirror của AUTH_USER_PROFILE từ iam-identity-service (chỉ field cần thiết)
@Entity
@Table(name = "AUTH_USER_PROFILE")
@Getter
@NoArgsConstructor
public class AuthUserProfile {

    @Id
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "EMAIL_PERSONAL")
    private String emailPersonal;

    @Column(name = "EMPLOYEE_CODE")
    private String employeeCode;
}
