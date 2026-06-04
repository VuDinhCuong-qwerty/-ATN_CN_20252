package com.iam.identity.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {

    private Long   userId;
    private String employeeCode;
    private String username;
    private String email;
    private String emailPersonal;
    private String mobile;
    private String positionCode;
    private String position;
    private String status;
    private String firstName;
    private String lastName;
    private String fullName;
    private String displayName;
    private String gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private String nationality;
    private String ethnic;
    private String religion;
    private String avatarUrl;
    private String numberId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate numberIdIssuedDate;

    private String numberIdIssuedPlace;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinDate;

    private List<AddressInfo>    addresses;
    private List<DepartmentInfo> departments;
    private List<RoleInfo>       roles;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressInfo {
        private String type;
        private Long   wardCode;
        private Long   provinceCode;
        private String wardName;
        private String provinceName;
        private String detail;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentInfo {
        private Long   departmentId;
        private String code;
        private String name;
        private Long   parentId;
        private int    depth;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private String roleCode;
        private String roleName;
    }
}
