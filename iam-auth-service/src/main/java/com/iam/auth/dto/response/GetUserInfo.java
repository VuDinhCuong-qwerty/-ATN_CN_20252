package com.iam.auth.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetUserInfo {
    // OIDC required — must match sub in ID Token
    private String sub;

    // thông tin cá nhân
    private String employeeCode;
    private String fullName;
    private String firstName;
    private String lastName;
    private String displayName;
    private String gender;
    private Integer age;
    private Instant dob;
    private String numberId;
    private String numberIdIssuePlace;
    private LocalDate numberIdIssueDate;
    private String nationality;
    private String ethnic;
    private String religion;
    private String avatarUrl;
    private Address permanentAddress;
    private Address temporaryAddress;
    private Address birthPlace;
    private String status;

    // thông tin liên lạc
    private String email;
    private String mobile;

    // thông tin công việc
    private LocalDate joinDate;
    private LocalDate leaveDate;
    private String position;
    private List<Role> roles;
    private Department department;

    // permissions cho app hiện tại — dùng để ES OIDC realm map roles
    private List<String> permissions;

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public class Department {
        private Long id;
        private String code;
        private String name;
        private String details;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public class Address {
        private Long provinceCode;
        private String provinceName;
        private Long wardCode;
        private String wardName;
        private String detailAddress;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public class Role {
        private Long id;
        private String code;
        private String name;
    }

    public interface STATUS {
        String ACTIVE = "ACTIVE";
        String INACTIVE = "INACTIVE";
        String ON_LEAVE = "ON_LEAVE";
    }

}

/*
 * select
 * uf.full_name as full_name,
 * uf.first_name as first_name,
 * uf.last_name as last_name,
 * uf.display_name as display_name,
 * uf.gender as gender,
 * uf.dob as dob,
 * uf.number_id as number_id,
 * 
 * from auth_user u
 * join auth_user_profile uf on u.id = uf.user_id
 * join auth_user_address ua on ua.user_id = u.id
 * join auth_province p on p.code = ua.province_code
 * join auth_ward w on w.code = ua.ward_code
 * join auth_department d on d.id = uf.department_id
 * join auth_user_role ur on ur.user_id = u.id
 * join auth_role r on r.id = ur.role_id
 * where u.id = 2;
 */
