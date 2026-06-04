package com.iam.identity.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AUTH_USER_PROFILE")
@org.hibernate.annotations.DynamicUpdate
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserProfile {

    @Id
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    @Column(name = "GENDER")
    private String gender;

    @Column(name = "DOB")
    private LocalDate dob;

    @Column(name = "EMAIL_PERSONAL")
    private String emailPersonal;

    @Column(name = "NATIONALITY")
    private String nationality;

    @Column(name = "ETHNIC")
    private String ethnic;

    @Column(name = "RELIGION")
    private String religion;

    @Column(name = "AVATAR_URL")
    private String avatarUrl;

    @Column(name = "CCCD")
    private String cccd;

    @Column(name = "CCCD_ISSUED_DATE")
    private LocalDate cccdIssuedDate;

    @Column(name = "CCCD_ISSUED_PLACE")
    private String cccdIssuedPlace;

    @Column(name = "EMPLOYEE_CODE")
    private String employeeCode;

    @Column(name = "JOIN_DATE")
    private LocalDate joinDate;

    @Column(name = "DEPARTMENT_ID")
    private Long departmentId;

    @Column(name = "POSITION")
    private String position;

    @Column(name = "CREATED_AT")
    private Instant createdAt;

    @Column(name = "UPDATED_AT")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

/*
 * ( "USER_ID" NUMBER NOT NULL ENABLE,
 * "FIRST_NAME" VARCHAR2(100 BYTE),
 * "LAST_NAME" VARCHAR2(100 BYTE),
 * "FULL_NAME" VARCHAR2(200 BYTE),
 * "DISPLAY_NAME" VARCHAR2(200 BYTE),
 * "GENDER" VARCHAR2(10 BYTE),
 * "DOB" DATE,
 * "NATIONALITY" VARCHAR2(100 BYTE),
 * "ETHNIC" VARCHAR2(100 BYTE),
 * "RELIGION" VARCHAR2(100 BYTE),
 * "AVATAR_URL" VARCHAR2(500 BYTE),
 * "CCCD" VARCHAR2(20 BYTE),
 * "CCCD_ISSUED_DATE" DATE,
 * "CCCD_ISSUED_PLACE" VARCHAR2(200 BYTE),
 * "EMPLOYEE_CODE" VARCHAR2(50 BYTE),
 * "JOIN_DATE" DATE,
 * "DEPARTMENT_ID" NUMBER,
 * "POSITION" VARCHAR2(200 BYTE),
 * "CREATED_AT" TIMESTAMP (6) DEFAULT CURRENT_TIMESTAMP NOT NULL ENABLE,
 * "UPDATED_AT" TIMESTAMP (6) DEFAULT CURRENT_TIMESTAMP NOT NULL ENABLE,
 */