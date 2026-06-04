package com.iam.identity.dto.pojo;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoRow {
    private Long      userId;
    private String    employeeCode;
    private String    username;
    private String    email;
    private String    emailPersonal;
    private String    mobile;
    private String    positionCode;
    private String    position;
    private String    status;
    private String    firstName;
    private String    lastName;
    private String    fullName;
    private String    displayName;
    private String    gender;
    private LocalDate dob;
    private String    nationality;
    private String    ethnic;
    private String    religion;
    private String    avatarUrl;
    private String    numberId;
    private LocalDate numberIdIssuedDate;
    private String    numberIdIssuedPlace;
    private LocalDate joinDate;
    private Long departmentId;
}
