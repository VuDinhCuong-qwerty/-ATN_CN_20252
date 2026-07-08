package com.iam.ldap.repository;

import com.iam.ldap.model.ApplicationEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Truy vấn AUTH_APPLICATION và AUTH_APP_PERMISSION qua JDBC.
 * JdbcTemplate thay thế EntityManager — không dùng JPA/ORM.
 */
@Repository
public class ApplicationRepository {

    private final JdbcTemplate jdbc;

    public ApplicationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── SQL base ──────────────────────────────────────────────────────────────

    private static final String BASE_SQL = """
            SELECT ID, SERVICE_CODE, NAME, DEFAULT_URL, APP_TYPE, STATUS
            FROM AUTH_APPLICATION
            WHERE STATUS = 'ACTIVE'
            """;

    /** Map 1 row ResultSet → ApplicationEntry (không có members — service sẽ bổ sung). */
    private static final RowMapper<ApplicationEntry> ROW_MAPPER = (rs, rowNum) ->
            ApplicationEntry.builder()
                    .id(rs.getLong("ID"))
                    .serviceCode(rs.getString("SERVICE_CODE"))
                    .name(rs.getString("NAME"))
                    .defaultUrl(rs.getString("DEFAULT_URL"))
                    .appType(rs.getString("APP_TYPE"))
                    .status(rs.getString("STATUS"))
                    .build();

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Lấy toàn bộ application ACTIVE — dùng cho SEARCH ou=groups. */
    public List<ApplicationEntry> findAll() {
        return jdbc.query(BASE_SQL, ROW_MAPPER);
    }

    /**
     * Tìm 1 application theo serviceCode — dùng cho LOOKUP cn=serviceCode.
     */
    public Optional<ApplicationEntry> findByServiceCode(String serviceCode) {
        String sql = BASE_SQL + """
                AND SERVICE_CODE = ?
                """;
        List<ApplicationEntry> results = jdbc.query(sql, ROW_MAPPER, serviceCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Kiểm tra app có tồn tại và ACTIVE theo serviceCode.
     * Dùng cho hasEntry() khi gặp virtual service OU: ou=gitlab-server,ou=users,...
     * Chỉ cần biết tồn tại hay không — không cần load full entry.
     *
     * Logic: COUNT(*) > 0 ↔ app tồn tại
     */
    public boolean existsByServiceCode(String serviceCode) {
        String sql = """
                SELECT COUNT(*)
                FROM AUTH_APPLICATION
                WHERE SERVICE_CODE = ?
                  AND STATUS       = 'ACTIVE'
                """;
        Integer count = jdbc.queryForObject(sql, Integer.class, serviceCode);
        return count != null && count > 0;
    }

    /**
     * Tìm các application mà user (theo USERNAME) có AUTH_APP_PERMISSION ACTIVE.
     * Dùng khi GitLab search groups với filter (member=uid=USERNAME,...).
     */
    public List<ApplicationEntry> findByMember(String username) {
        String sql = BASE_SQL + """
                AND ID IN (
                    SELECT ap.APP_ID
                    FROM AUTH_APP_PERMISSION ap
                    JOIN AUTH_USER u ON u.ID = ap.USER_ID
                    WHERE u.USERNAME = ?
                      AND ap.STATUS  = 'ACTIVE'
                      AND u.STATUS   = 'ACTIVE'
                      AND (ap.EXPIRED_AT IS NULL OR ap.EXPIRED_AT > SYSDATE)
                )
                """;
        return jdbc.query(sql, ROW_MAPPER, username);
    }

    /**
     * Lấy danh sách USERNAME của user có quyền ACTIVE trong app này.
     * Service sẽ build thành member DN với service-encoded format:
     *   uid={username},ou={serviceCode},ou=users,{suffix}
     */
    public List<String> findUsernamesByAppId(Long appId) {
        String sql = """
                SELECT u.USERNAME
                FROM AUTH_USER u
                JOIN AUTH_APP_PERMISSION ap ON ap.USER_ID = u.ID
                WHERE ap.APP_ID = ?
                  AND ap.STATUS = 'ACTIVE'
                  AND u.STATUS  = 'ACTIVE'
                  AND (ap.EXPIRED_AT IS NULL OR ap.EXPIRED_AT > SYSDATE)
                """;
        return jdbc.queryForList(sql, String.class, appId);
    }
}
