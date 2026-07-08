package com.iam.ldap.repository;

import com.iam.ldap.model.UserEntry;

import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn AUTH_USER JOIN AUTH_USER_PROFILE qua JDBC.
 * JdbcTemplate thay thế EntityManager — không dùng JPA/ORM.
 */
@Repository
@Slf4j
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── SQL base ──────────────────────────────────────────────────────────────

    private static final String BASE_SQL = """
            SELECT u.ID, u.USERNAME, u.PASSWORD, u.EMAIL, u.DISPLAY_NAME, u.MOBILE,
                   u.STATUS, u.FORCE_CHANGE_PASSWORD,
                   p.FIRST_NAME, p.LAST_NAME, p.FULL_NAME,
                   p.EMPLOYEE_CODE, p.DEPARTMENT_ID, p.POSITION, p.DOB, p.EMAIL_PERSONAL
            FROM AUTH_USER u
            LEFT JOIN AUTH_USER_PROFILE p ON p.USER_ID = u.ID
            WHERE u.STATUS = 'ACTIVE'
            """;

    /** Map 1 row ResultSet → UserEntry. */
    private static final RowMapper<UserEntry> ROW_MAPPER = (rs, rowNum) -> {
        Date dobSql = rs.getDate("DOB");
        return UserEntry.builder()
                .id(rs.getLong("ID"))
                .username(rs.getString("USERNAME"))
                .password(rs.getString("PASSWORD"))
                .email(rs.getString("EMAIL"))
                .displayName(rs.getString("DISPLAY_NAME"))
                .mobile(rs.getString("MOBILE"))
                .status(rs.getString("STATUS"))
                .forceChangePassword(rs.getObject("FORCE_CHANGE_PASSWORD", Integer.class))
                .firstName(rs.getString("FIRST_NAME"))
                .lastName(rs.getString("LAST_NAME"))
                .fullName(rs.getString("FULL_NAME"))
                .employeeCode(rs.getString("EMPLOYEE_CODE"))
                .departmentId(rs.getObject("DEPARTMENT_ID", Long.class))
                .position(rs.getString("POSITION"))
                .dob(dobSql != null ? dobSql.toLocalDate() : null)
                .emailPersonal(rs.getString("EMAIL_PERSONAL"))
                .build();
    };

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Tìm 1 user theo username — dùng cho BIND và LOOKUP (old-style DN không có service OU).
     */
    public Optional<UserEntry> findByUsername(String username) {
        String sql = BASE_SQL + """
                AND u.USERNAME = ?
                """;
        List<UserEntry> results = jdbc.query(sql, ROW_MAPPER, username);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Lấy toàn bộ user ACTIVE — dùng cho SEARCH khi base là ou=users (không có service filter).
     */
    public List<UserEntry> findAll() {
        return jdbc.query(BASE_SQL, ROW_MAPPER);
    }

    /**
     * Lấy toàn bộ user ACTIVE có quyền truy cập app theo serviceCode.
     * Dùng cho SEARCH khi base DN chứa service OU:
     *   base = ou=gitlab-server,ou=users,dc=... → serviceCode = "gitlab-server"
     *
     * Logic:
     *   1. Lấy user ACTIVE từ AUTH_USER + AUTH_USER_PROFILE
     *   2. Filter EXISTS trong AUTH_APP_PERMISSION cho service tương ứng
     */
    public List<UserEntry> findAllByService(String serviceCode) {
        log.info("ServiceCode='{}'", serviceCode);
        String sql = BASE_SQL + """
                AND EXISTS (
                    SELECT 1
                    FROM AUTH_APP_PERMISSION ap
                    JOIN AUTH_APPLICATION a ON a.ID = ap.APP_ID
                    WHERE ap.USER_ID     = u.ID
                      AND a.SERVICE_CODE = ?
                      AND ap.STATUS      = 'ACTIVE'
                      AND a.STATUS       = 'ACTIVE'
                )
                """;
        var result = jdbc.query(sql, ROW_MAPPER, serviceCode);
        log.info("Result search: {}", result);
        return result;
    }

    /**
     * Tìm 1 user theo username VÀ kiểm tra quyền truy cập service cùng lúc.
     * Dùng cho LOOKUP / hasEntry khi DN dạng:
     *   uid=CUONGVD,ou=gitlab-server,ou=users,...
     *
     * Logic:
     *   1. Thêm điều kiện USERNAME = ? vào findAllByService
     *   2. Trả Optional<UserEntry> — empty nếu không tồn tại hoặc không có quyền
     */
    public Optional<UserEntry> findByUsernameAndService(String username, String serviceCode) {
        String sql = BASE_SQL + """
                AND u.USERNAME = ?
                AND EXISTS (
                    SELECT 1
                    FROM AUTH_APP_PERMISSION ap
                    JOIN AUTH_APPLICATION a ON a.ID = ap.APP_ID
                    WHERE ap.USER_ID     = u.ID
                      AND a.SERVICE_CODE = ?
                      AND ap.STATUS      = 'ACTIVE'
                      AND a.STATUS       = 'ACTIVE'
                )
                """;
        List<UserEntry> results = jdbc.query(sql, ROW_MAPPER, username, serviceCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Kiểm tra user có AUTH_APP_PERMISSION ACTIVE cho app theo serviceCode.
     * Dùng trong OracleAuthenticator sau khi BCrypt verify thành công.
     */
    public boolean hasAppPermission(String username, String serviceCode) {
        String sql = """
                SELECT COUNT(*)
                FROM AUTH_APP_PERMISSION ap
                JOIN AUTH_USER u        ON u.ID = ap.USER_ID
                JOIN AUTH_APPLICATION a ON a.ID = ap.APP_ID
                WHERE u.USERNAME     = ?
                  AND a.SERVICE_CODE = ?
                  AND ap.STATUS      = 'ACTIVE'
                  AND u.STATUS       = 'ACTIVE'
                  AND a.STATUS       = 'ACTIVE'
                """;
        Integer count = jdbc.queryForObject(sql, Integer.class, username, serviceCode);
        return count != null && count > 0;
    }

    /**
     * Lấy danh sách SERVICE_CODE của các app mà user có quyền ACTIVE.
     * Dùng để build memberOf DN list trong UserService.enrichWithMemberOf().
     */
    public List<String> findAppCodesByUserId(Long userId) {
        String sql = """
                SELECT a.SERVICE_CODE
                FROM AUTH_APP_PERMISSION ap
                JOIN AUTH_APPLICATION a ON a.ID = ap.APP_ID
                WHERE ap.USER_ID = ?
                  AND ap.STATUS  = 'ACTIVE'
                  AND a.STATUS   = 'ACTIVE'
                  AND (ap.EXPIRED_AT IS NULL OR ap.EXPIRED_AT > SYSDATE)
                """;
        return jdbc.queryForList(sql, String.class, userId);
    }

    /**
     * Lấy danh sách permission strings của user từ AUTH_USER_RESOURCE.
     * Format output: "serviceCode/resourceCode:action" (1 string per action trong CSV).
     * Dùng để build description attribute trong LDAP user entry → ES metadata.description.
     *
     * Logic:
     *   1. JOIN AUTH_USER_RESOURCE → AUTH_RESOURCE → AUTH_APPLICATION
     *   2. Với mỗi row: split ACTION (CSV "view,edit") thành từng action riêng
     *   3. Build string: serviceCode + "/" + resourceCode + ":" + action
     */
    public List<String> findPermissionsByUserId(Long userId) {
        String sql = """
                SELECT a.SERVICE_CODE, r.RESOURCE_CODE, ur.ACTION
                FROM AUTH_USER_RESOURCE ur
                JOIN AUTH_RESOURCE r    ON r.ID = ur.RESOURCE_ID
                JOIN AUTH_APPLICATION a ON a.ID = r.APP_ID
                WHERE ur.USER_ID = ?
                  AND ur.STATUS  = 'ACTIVE'
                  AND r.STATUS   = 'ACTIVE'
                  AND a.STATUS   = 'ACTIVE'
                  AND (ur.EXPIRED_AT IS NULL OR ur.EXPIRED_AT > SYSDATE)
                """;
        List<String> permissions = new ArrayList<>();
        jdbc.query(sql, rs -> {
            String serviceCode  = rs.getString("SERVICE_CODE");
            String resourceCode = rs.getString("RESOURCE_CODE");
            String actionCsv    = rs.getString("ACTION");
            if (actionCsv == null || actionCsv.isBlank()) return;
            for (String action : actionCsv.split(",")) {
                action = action.trim();
                if (!action.isEmpty()) {
                    permissions.add(serviceCode + "/" + resourceCode + ":" + action);
                }
            }
        }, userId);
        return permissions;
    }
}
