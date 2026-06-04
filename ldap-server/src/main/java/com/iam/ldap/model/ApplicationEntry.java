package com.iam.ldap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * POJO ánh xạ từ AUTH_APPLICATION.
 * Không phải @Entity JPA — chỉ là kết quả ánh xạ từ JDBC ResultSet.
 *
 * LDAP DN: cn={serviceCode},ou=groups,dc=iam,dc=bank,dc=vn
 * objectClass: groupOfNames
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEntry {

    // ── Từ AUTH_APPLICATION ───────────────────────────────────────────────────

    private Long id;

    /** LDAP attribute: cn — RDN của group entry */
    private String serviceCode;

    /** LDAP attribute: description */
    private String name;

    /** LDAP attribute: labeledUri */
    private String defaultUrl;

    /** INTERNAL | THIRD_PARTY_LDAP */
    private String appType;

    /** ACTIVE | INACTIVE — Repository chỉ trả ACTIVE */
    private String status;

    // ── Populate từ AUTH_APP_PERMISSION JOIN AUTH_USER ────────────────────────

    /**
     * LDAP attribute: member (multi-value, bắt buộc với objectClass groupOfNames).
     * Format: "uid={username},ou=users,dc=iam,dc=bank,dc=vn"
     * Chỉ lấy user có permission ACTIVE và chưa hết hạn.
     */
    private List<String> members;
}
