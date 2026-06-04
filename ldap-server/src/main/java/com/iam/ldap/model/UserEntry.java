package com.iam.ldap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * POJO kết hợp dữ liệu từ AUTH_USER JOIN AUTH_USER_PROFILE.
 * Không phải @Entity JPA — chỉ là kết quả ánh xạ từ JDBC ResultSet.
 *
 * LDAP DN: uid={username},ou=users,dc=iam,dc=bank,dc=vn
 * objectClass: inetOrgPerson
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntry {

    // ── Từ AUTH_USER ──────────────────────────────────────────────────────────

    private Long id;

    /** LDAP attribute: uid — RDN của entry, GitLab dùng để lookup user */
    private String username;

    /** BCrypt hash — chỉ dùng để verify BIND, KHÔNG expose ra LDAP entry */
    private String password;

    /** LDAP attribute: mail */
    private String email;

    /** LDAP attribute: displayName */
    private String displayName;

    /** LDAP attribute: mobile */
    private String mobile;

    /** ACTIVE | INACTIVE | DELETED — Repository chỉ trả ACTIVE */
    private String status;

    private Integer forceChangePassword;

    // ── Từ AUTH_USER_PROFILE (LEFT JOIN) ──────────────────────────────────────

    /** LDAP attribute: givenName */
    private String firstName;

    /** LDAP attribute: sn */
    private String lastName;

    /** LDAP attribute: cn */
    private String fullName;

    /** LDAP attribute: employeeNumber */
    private String employeeCode;

    /** LDAP attribute: departmentNumber */
    private Long departmentId;

    /** LDAP attribute: title */
    private String position;

    private LocalDate dob;

    private String emailPersonal;

    // ── Populate từ AUTH_APP_PERMISSION JOIN AUTH_APPLICATION ─────────────────

    /**
     * LDAP attribute: memberOf (multi-value).
     * Format: "cn={serviceCode},ou=groups,dc=iam,dc=bank,dc=vn"
     * GitLab dùng để kiểm tra user có thuộc group nào không.
     */
    private List<String> memberOf;

    // ── Populate từ AUTH_USER_RESOURCE JOIN AUTH_RESOURCE JOIN AUTH_APPLICATION ─

    /**
     * LDAP attribute: description (multi-value).
     * Format: "serviceCode/resourceCode:action"  vd: "kibana-server/iam-system-logs:view"
     * ES LDAP realm đọc qua metadata.description → dùng cho role mapping.
     */
    private List<String> permissions;
}
