package com.iam.ldap.converter;

import com.iam.ldap.model.ApplicationEntry;
import com.iam.ldap.model.UserEntry;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Chuyển đổi POJO (UserEntry / ApplicationEntry) → ApacheDS Entry.
 *
 * SchemaManager được truyền vào từng method (không inject vào class)
 * vì nó chỉ có sau khi DirectoryService khởi động xong.
 * OraclePartition có sẵn this.schemaManager (kế thừa từ AbstractPartition)
 * và truyền vào khi gọi converter.
 *
 * Pipeline:
 *   Oracle SQL result → POJO → Entry → ApacheDS encode → LDAP response
 */
@Component
public class EntryConverter {

    private static final Logger log = LoggerFactory.getLogger(EntryConverter.class);

    @Value("${ldap.server.suffix:dc=iam,dc=bank,dc=vn}")
    private String suffix;

    // ═══════════════════════════════════════════════════════════════════════════
    //  User: inetOrgPerson
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Overload không có serviceCode — backward compat, build DN flat (ou=users).
     * Gọi toUserEntry(user, schemaManager, null).
     */
    public Entry toUserEntry(UserEntry user, SchemaManager schemaManager) throws LdapException {
        return toUserEntry(user, schemaManager, null);
    }

    /**
     * Chuyển UserEntry → LDAP Entry với objectClass inetOrgPerson.
     *
     * Logic DN (Approach B):
     *   1. serviceCode != null → uid={username},ou={serviceCode},ou=users,{suffix}
     *   2. serviceCode == null → uid={username},ou=users,{suffix}  (backward compat)
     *
     * Mapping attribute:
     *   uid              ← username
     *   cn               ← fullName (fallback: displayName → username)
     *   sn               ← lastName  (fallback: cn)
     *   givenName        ← firstName
     *   displayName      ← displayName
     *   mail             ← email
     *   mobile           ← mobile
     *   employeeNumber   ← employeeCode
     *   departmentNumber ← departmentId
     *   title            ← position
     */
    public Entry toUserEntry(UserEntry user, SchemaManager schemaManager, String serviceCode) throws LdapException {
        // 1. Build DN — service-encoded khi có serviceCode, flat khi không
        String dn;
        if (serviceCode != null && !serviceCode.isBlank()) {
            dn = "uid=" + user.getUsername() + ",ou=" + serviceCode + ",ou=users," + suffix;
        } else {
            dn = "uid=" + user.getUsername() + ",ou=users," + suffix;
        }

        DefaultEntry entry = new DefaultEntry(schemaManager, dn);

        // objectClass — inetOrgPerson kế thừa person → organizationalPerson → top
        // extensibleObject cho phép thêm memberOf (không thuộc inetOrgPerson standard schema)
        entry.add("objectClass", "top", "person", "organizationalPerson", "inetOrgPerson", "extensibleObject");

        // uid — RDN, bắt buộc
        entry.add("uid", user.getUsername());

        // cn — required bởi objectClass person
        String cn = firstNonNull(user.getFullName(), user.getDisplayName(), user.getUsername());
        entry.add("cn", cn);

        // sn — required bởi objectClass person
        String sn = firstNonNull(user.getLastName(), cn);
        entry.add("sn", sn);

        // Optional attributes — chỉ thêm khi có giá trị
        addIfPresent(entry, "givenName",        user.getFirstName());
        addIfPresent(entry, "displayName",       user.getDisplayName());
        addIfPresent(entry, "mail",              user.getEmail());
        addIfPresent(entry, "mobile",            user.getMobile());
        addIfPresent(entry, "employeeNumber",    user.getEmployeeCode());
        addIfPresent(entry, "title",             user.getPosition());

        if (user.getDepartmentId() != null) {
            entry.add("departmentNumber", String.valueOf(user.getDepartmentId()));
        }

        // userPassword — KHÔNG expose ra LDAP entry.
        // OracleAuthenticator tự query Oracle để verify BCrypt hash.
        // SimpleAuthenticator đã bị xóa khỏi interceptor nên không cần placeholder.

        // memberOf KHÔNG thêm vào entry:
        // ApacheDS schema không có attribute type này (AD-specific).
        // GitLab tìm group membership qua attribute 'member' trong group entry (ou=groups).

        // description — multi-value: permission strings "serviceCode/resourceCode:action"
        // ES LDAP realm đọc qua metadata.description → API role mapping dùng để assign ES roles.
        if (user.getPermissions() != null) {
            for (String perm : user.getPermissions()) {
                entry.add("description", perm);
            }
        }

        log.debug("Converted UserEntry '{}' → LDAP Entry DN='{}' (serviceCode={}, permissions={})",
                user.getUsername(), dn, serviceCode,
                user.getPermissions() != null ? user.getPermissions().size() : 0);
        return entry;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Application: groupOfNames
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Chuyển ApplicationEntry → LDAP Entry với objectClass groupOfNames.
     *
     * DN:  cn={serviceCode},ou=groups,dc=iam,dc=bank,dc=vn
     *
     * Mapping attribute:
     *   cn          ← serviceCode  (RDN)
     *   description ← name
     *   labeledURI  ← defaultUrl
     *   member      ← list DN của user có quyền ACTIVE (multi-value, bắt buộc)
     */
    public Entry toApplicationEntry(ApplicationEntry app, SchemaManager schemaManager) throws LdapException {
        String dn = "cn=" + app.getServiceCode() + ",ou=groups," + suffix;
        DefaultEntry entry = new DefaultEntry(schemaManager, dn);

        // objectClass — groupOfNames yêu cầu ít nhất 1 attribute member
        entry.add("objectClass", "top", "groupOfNames");

        // cn — RDN, bắt buộc
        entry.add("cn", app.getServiceCode());

        addIfPresent(entry, "description", app.getName());
        addIfPresent(entry, "labeledURI",  app.getDefaultUrl());

        // member — bắt buộc với groupOfNames (schema constraint)
        // Nếu không có member nào, dùng DN của chính group làm placeholder
        // để thoả mãn schema mà không làm sai ngữ nghĩa
        if (app.getMembers() != null && !app.getMembers().isEmpty()) {
            entry.add("member", app.getMembers().toArray(new String[0]));
        } else {
            entry.add("member", dn);
        }

        log.debug("Converted ApplicationEntry '{}' → LDAP Entry DN='{}'", app.getServiceCode(), dn);
        return entry;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helper
    // ═══════════════════════════════════════════════════════════════════════════

    /** Thêm attribute chỉ khi value khác null và không rỗng. */
    private void addIfPresent(DefaultEntry entry, String attrName, String value) throws LdapException {
        if (value != null && !value.isBlank()) {
            entry.add(attrName, value);
        }
    }

    /** Trả về giá trị đầu tiên khác null trong danh sách. */
    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
