package com.iam.ldap.authenticator;

import com.iam.ldap.service.UserService;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Xử lý LDAP BIND request cho user entries trong ou=users.
 *
 * Thay thế SimpleAuthenticator mặc định của ApacheDS (so sánh plaintext).
 * Dùng BCrypt để verify mật khẩu được hash trong Oracle.
 *
 * Flow BIND:
 *   GitLab gửi: BIND dn="uid=john,ou=users,dc=iam,dc=bank,dc=vn" password="abc123"
 *     → ApacheDS gọi OracleAuthenticator.authenticate()
 *     → parse DN → lấy username = "john"
 *     → UserService.findByUsername("john") → lấy BCrypt hash từ Oracle
 *     → BCrypt.matches("abc123", hash)
 *     → true  → trả LdapPrincipal → GitLab login thành công
 *     → false → throw LdapAuthenticationException → GitLab báo sai mật khẩu
 *
 * Chú ý: Admin BIND (uid=admin,ou=system) KHÔNG đi qua đây —
 * isValid() chỉ trả true cho DN thuộc ou=users.
 * Admin được xử lý bởi SimpleAuthenticator mặc định của ApacheDS.
 */
@Component
public class OracleAuthenticator extends AbstractAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(OracleAuthenticator.class);

    private final UserService userService;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Value("${ldap.server.admin.password:secret}")
    private String adminPassword;

    // Service code của app mà user phải có AUTH_APP_PERMISSION.
    // Để trống = không kiểm tra (mọi ACTIVE user đều BIND được).
    @Value("${ldap.server.required-app:}")
    private String requiredApp;

    public OracleAuthenticator(UserService userService) {
        // SIMPLE = username + password (phân biệt với STRONG = certificate)
        super(AuthenticationLevel.SIMPLE);
        this.userService = userService;
    }

    /**
     * Xử lý tất cả SIMPLE bind sau khi SimpleAuthenticator bị gỡ khỏi interceptor.
     * - ou=users → verify BCrypt qua Oracle
     * - ou=system → verify plaintext admin password từ config
     */
    @Override
    public boolean isValid(Dn dn) {
        log.debug("isValid dn={}", dn.toString());
        String dnStr = dn.toString().toLowerCase();
        return dnStr.contains("ou=users") || dnStr.contains("ou=system");
    }

    /**
     * Xác thực user bằng BCrypt.
     *
     * @param ctx chứa DN và password (dạng byte[]) do client gửi lên
     * @return LdapPrincipal nếu xác thực thành công
     * @throws LdapAuthenticationException nếu sai DN, sai password, tài khoản inactive
     */
    @Override
    public LdapPrincipal authenticate(BindOperationContext ctx) throws LdapException {
        Dn dn = ctx.getDn();
        String dnStr = dn.toString().toLowerCase();

        // Admin bind (uid=admin,ou=system) — verify plaintext từ config
        if (dnStr.contains("ou=system")) {
            return authenticateAdmin(ctx, dn);
        }

        // User bind (uid=...,ou=users) — verify BCrypt từ Oracle
        return authenticateUser(ctx, dn);
    }

    private LdapPrincipal authenticateAdmin(BindOperationContext ctx, Dn dn) throws LdapException {
        byte[] credBytes = ctx.getCredentials();
        if (credBytes == null || credBytes.length == 0) {
            throw new LdapAuthenticationException("Empty credentials");
        }
        String input = new String(credBytes, StandardCharsets.UTF_8);
        if (!adminPassword.equals(input)) {
            log.warn("BIND rejected — wrong admin password");
            throw new LdapAuthenticationException("Invalid credentials");
        }
        log.debug("BIND success — admin");
        return new LdapPrincipal(getDirectoryService().getSchemaManager(), dn, AuthenticationLevel.SIMPLE);
    }

    private LdapPrincipal authenticateUser(BindOperationContext ctx, Dn dn) throws LdapException {
        // ── 1. Lấy username từ RDN ────────────────────────────────────────────
        String username = dn.getRdn().getValue().toUpperCase();
        log.debug("BIND attempt — dn='{}', username='{}'", dn, username);

        // ── 2. Lấy password từ credentials ───────────────────────────────────
        byte[] credBytes = ctx.getCredentials();
        if (credBytes == null || credBytes.length == 0) {
            log.warn("BIND rejected — empty credentials for '{}'", username);
            throw new LdapAuthenticationException("Empty credentials for user: " + username);
        }
        String inputPassword = new String(credBytes, StandardCharsets.UTF_8);

        // ── 3. Tìm user trong Oracle ──────────────────────────────────────────
        // UserService.findByUsername() sẽ được implement ở bước tiếp theo
        var userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("BIND rejected — user '{}' not found", username);
            // Dùng cùng message để tránh user enumeration attack
            throw new LdapAuthenticationException("Invalid credentials");
        }
        var user = userOpt.get();

        // ── 4. Kiểm tra trạng thái tài khoản ─────────────────────────────────
        if (!"ACTIVE".equals(user.getStatus())) {
            log.warn("BIND rejected — account '{}' is {}", username, user.getStatus());
            throw new LdapAuthenticationException("Account is not active");
        }

        // ── 5. BCrypt verify ──────────────────────────────────────────────────
        if (user.getPassword() == null || !bcrypt.matches(inputPassword, user.getPassword())) {
            log.warn("BIND rejected — wrong password for '{}'", username);
            throw new LdapAuthenticationException("Invalid credentials");
        }

        // ── 6. Kiểm tra quyền truy cập app (Approach B) ──────────────────────
        // Ưu tiên: đọc service code từ DN path (uid=X,ou=SERVICE,ou=users,...)
        // Fallback: requiredApp config (backward compat khi dùng old-style DN)
        String serviceCode = extractServiceFromUserDn(dn);
        if (serviceCode != null) {
            if (!userService.hasAppPermission(username, serviceCode)) {
                log.warn("BIND rejected — '{}' has no permission for service '{}' (from DN)", username, serviceCode);
                throw new LdapAuthenticationException("Not authorized for this application");
            }
        } else if (requiredApp != null && !requiredApp.isBlank()) {
            if (!userService.hasAppPermission(username, requiredApp)) {
                log.warn("BIND rejected — '{}' has no permission for app '{}' (from config)", username, requiredApp);
                throw new LdapAuthenticationException("Not authorized for this application");
            }
        }

        // ── 7. Xác thực thành công ────────────────────────────────────────────
        log.info("BIND success — user='{}'", username);
        return new LdapPrincipal(
                getDirectoryService().getSchemaManager(),
                dn,
                AuthenticationLevel.SIMPLE
        );
    }

    /**
     * Đọc service code từ user entry DN (dùng chung logic với OraclePartition).
     *
     * Logic:
     *   1. Lấy parent RDN (direct parent OU của user)
     *   2. Parent RDN type phải là "ou"
     *   3. Parent RDN value khác "users" / "groups" → đây là service OU
     *   4. Return parent RDN value (service code)
     *
     * Ví dụ:
     *   uid=CUONGVD,ou=gitlab-server,ou=users,dc=... → "gitlab-server"
     *   uid=CUONGVD,ou=users,dc=...                  → null
     */
    private String extractServiceFromUserDn(Dn dn) {
        // 1. Lấy parent RDN
        try {
            if (dn == null) return null;
            Dn parent = dn.getParent();
            if (parent == null) return null;
            String parentType  = parent.getRdn().getType();
            String parentValue = parent.getRdn().getValue();

            // 2. Parent phải là "ou" không phải "users" / "groups"
            if ("ou".equalsIgnoreCase(parentType)
                    && !"users".equalsIgnoreCase(parentValue)
                    && !"groups".equalsIgnoreCase(parentValue)) {
                return parentValue;
            }
            return null;
        } catch (Exception e) {
            log.warn("extractServiceFromUserDn: cannot parse DN '{}': {}", dn, e.getMessage());
            return null;
        }
    }
}
