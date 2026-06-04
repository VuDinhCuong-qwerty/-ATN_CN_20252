package com.iam.identity.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    // ── Thông tin cá nhân ─────────────────────────────────────────────────────

    @NotBlank
    @Size(max = 200)
    private String fullName;

    @NotNull
    private LocalDate dob;

    @NotBlank
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$")
    private String gender;

    @NotBlank
    @Size(max = 100)
    private String nationality;

    @NotBlank
    @Size(max = 100)
    private String ethnic;

    @NotBlank
    @Size(max = 100)
    private String religion;

    @Size(max = 500)
    private String avatarUrl;

    // ── CCCD ──────────────────────────────────────────────────────────────────

    @NotBlank
    @Pattern(regexp = "^\\d{12}$")
    private String numberId;

    @NotNull
    private LocalDate numberIdIssuedDate;

    @NotBlank
    @Size(max = 200)
    private String numberIdIssuedPlace;

    // ── Thông tin công việc ───────────────────────────────────────────────────

    @NotNull
    private Long departmentId;

    @NotNull
    private LocalDate joinDate;

    @Size(max = 200)
    private String position;

    @NotBlank
    @Pattern(regexp = "^(0\\d{9}|84\\d{9})$")
    private String mobile;

    @NotBlank
    @Email
    private String mail;

    // ── Địa chỉ ───────────────────────────────────────────────────────────────

    @Valid
    private Address temporaryAddress; // optional

    @Valid
    private Address permanentAddress; // optional

    @Valid
    private Address birthAddress; // optional

    // ── Phân quyền ────────────────────────────────────────────────────────────

    @NotEmpty
    @Valid
    private List<Role> roles;

    // ── Nested DTOs ───────────────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Role {
        @NotBlank
        private String code;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        @NotNull
        @Positive
        private Long provinceCode;

        @NotNull
        @Positive
        private Long wardCode;

        @NotBlank
        @Pattern(regexp = "^[\\p{L}0-9\\s,./-]+$")
        private String detail;
    }
}