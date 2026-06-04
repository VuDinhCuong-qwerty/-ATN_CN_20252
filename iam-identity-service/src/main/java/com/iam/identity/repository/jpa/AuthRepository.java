package com.iam.identity.repository.jpa;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Query;

import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.iam.identity.dto.pojo.DuplicateUserrData;
import com.iam.identity.dto.pojo.PermissionRequest;
import com.iam.identity.dto.pojo.Place;
import com.iam.identity.dto.pojo.ResourceRow;
import com.iam.identity.dto.pojo.UserAddressRow;
import com.iam.identity.dto.pojo.UserDepartmentRow;
import com.iam.identity.dto.pojo.UserInfoRow;
import com.iam.identity.dto.pojo.UserRoleRow;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRepository {

    private final EntityManager entityManager;

    @Transactional
    public List<DuplicateUserrData> checkDuplicateUserrData(String mobile, String numberId, String personalEmail) {

        String sql = """
                        SELECT
                            u.MOBILE as mobile,
                            up.CCCD as cccd
                        FROM AUTH_USER u
                        JOIN AUTH_USER_PROFILE up ON up.USER_ID = u.ID
                        WHERE 1 = 1
                            AND (u.MOBILE = :mobile OR up.CCCD = :numberID OR up.EMAIL_PERSONAL = :personalEmail)
                            AND u.STATUS <> 'DELETED'
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("mobile", mobile)
                .setParameter("numberID", numberId)
                .setParameter("personalEmail", personalEmail)
                .getResultList();

        if (rows.isEmpty())
            return List.of();

        return rows.stream()
                .map(row -> DuplicateUserrData.builder()
                        .mobile((String) row[0])
                        .numberId((String) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public Place getPlace(Long wardCode, Long provinceCode) {
        String sql = """
                SELECT
                    w.code    AS ward_code,
                    w.name    AS ward_name,
                    p.code    AS province_code,
                    p.name    AS province_name
                FROM auth_ward w
                JOIN auth_province p ON p.code = w.province_code
                WHERE 1 = 1
                    AND w.code         = :wardCode
                    AND p.code         = :provinceCode
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("wardCode", wardCode)
                .setParameter("provinceCode", provinceCode)
                .getResultList();

        if (rows.isEmpty())
            return null;

        Object[] row = rows.get(0);
        return Place.builder()
                .wardCode(String.valueOf(row[0]))
                .wardName((String) row[1])
                .provinceCode(String.valueOf(row[2]))
                .provinceName((String) row[3])
                .build();
    }

    @Transactional
    public int countValidUser(Long userId, String employeeCode) {
        String sql = """
                SELECT COUNT(*) FROM AUTH_USER u
                JOIN AUTH_USER_PROFILE up ON up.USER_ID = u.ID
                WHERE u.ID = :userId
                  AND up.EMPLOYEE_CODE = :employeeCode
                  AND u.STATUS <> 'DELETED'
                """;
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("employeeCode", employeeCode)
                .getSingleResult();
        return count.intValue();
    }

    // ── Procedure: lấy thông tin cơ bản user ─────────────────────────────────

    @Transactional
    public UserInfoRow getUserInfo(Long userId, String employeeCode) {
        return entityManager.unwrap(Session.class).doReturningWork(conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "{call IDENTITY_PKG.GET_USER_INFOR_BY_USER_ID(?, ?, ?)}")) {
                if (userId != null)
                    cs.setLong(1, userId);
                else
                    cs.setNull(1, Types.NUMERIC);

                if (employeeCode != null) {
                    cs.setString(2, employeeCode);
                } else
                    cs.setNull(2, Types.VARCHAR);

                cs.registerOutParameter(3, Types.REF_CURSOR);
                cs.execute();

                try (ResultSet rs = (ResultSet) cs.getObject(3)) {
                    if (rs != null && rs.next()) {
                        return UserInfoRow.builder()
                                .userId(toLong(rs.getObject("USER_ID")))
                                .employeeCode(rs.getString("EMPLOYEE_CODE"))
                                .username(rs.getString("USERNAME"))
                                .email(rs.getString("EMAIL"))
                                .emailPersonal(rs.getString("EMAIL_PERSONAL"))
                                .mobile(rs.getString("MOBILE"))
                                .positionCode(rs.getString("POSITION_CODE"))
                                .position(rs.getString("POSITION"))
                                .status(rs.getString("STATUS"))
                                .firstName(rs.getString("FIRST_NAME"))
                                .lastName(rs.getString("LAST_NAME"))
                                .fullName(rs.getString("FULL_NAME"))
                                .displayName(rs.getString("DISPLAY_NAME"))
                                .gender(rs.getString("GENDER"))
                                .dob(toLocalDate(rs.getObject("DOB")))
                                .nationality(rs.getString("NATIONALITY"))
                                .ethnic(rs.getString("ETHNIC"))
                                .religion(rs.getString("RELIGION"))
                                .avatarUrl(rs.getString("AVATAR_URL"))
                                .numberId(rs.getString("NUMBER_ID"))
                                .numberIdIssuedDate(toLocalDate(rs.getObject("NUMBER_ID_ISSUED_DATE")))
                                .numberIdIssuedPlace(rs.getString("NUMBER_ID_ISSUED_PLACE"))
                                .joinDate(toLocalDate(rs.getObject("JOIN_DATE")))
                                .departmentId(toLong(rs.getObject("DEPARTMENT_ID")))
                                .build();
                    }
                }
                return null;
            }
        });
    }

    // ── Native SQL: địa chỉ ───────────────────────────────────────────────────

    @Transactional
    public List<UserAddressRow> getAddressesByUserId(Long userId) {
        String sql = """
                SELECT
                    ad.TYPE       AS type,
                    w.CODE        AS ward_code,
                    prov.CODE     AS province_code,
                    w.NAME        AS ward_name,
                    prov.NAME     AS province_name,
                    ad.DETAIL     AS detail
                FROM AUTH_USER_ADDRESS ad
                JOIN AUTH_WARD     w    ON w.CODE    = ad.WARD_CODE
                JOIN AUTH_PROVINCE prov ON prov.CODE = ad.PROVINCE_CODE
                WHERE ad.USER_ID = :userId
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();

        return rows.stream()
                .map(r -> UserAddressRow.builder()
                        .type((String) r[0])
                        .wardCode(toLong(r[1]))
                        .provinceCode(toLong(r[2]))
                        .wardName((String) r[3])
                        .provinceName((String) r[4])
                        .detail((String) r[5])
                        .build())
                .collect(Collectors.toList());
    }

    // ── Native SQL: phòng ban (hierarchy, status = 1, tối đa 5 cấp) ──────────

    @Transactional
    public List<UserDepartmentRow> getDepartmentsByUserId(Long userId) {
        String sql = """
                SELECT
                    d.ID        AS department_id,
                    d.CODE      AS code,
                    d.NAME      AS name,
                    d.PARENT_ID AS parent_id,
                    LEVEL       AS depth
                FROM AUTH_DEPARTMENT d
                WHERE d.STATUS = 1
                START WITH d.ID = (
                    SELECT DEPARTMENT_ID FROM AUTH_USER_PROFILE WHERE USER_ID = :userId
                )
                CONNECT BY PRIOR d.PARENT_ID = d.ID AND LEVEL <= 5
                ORDER BY LEVEL
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();

        return rows.stream()
                .map(r -> UserDepartmentRow.builder()
                        .departmentId(toLong(r[0]))
                        .code((String) r[1])
                        .name((String) r[2])
                        .parentId(toLong(r[3]))
                        .depth(((Number) r[4]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Native SQL: vai trò đang active ──────────────────────────────────────

    @Transactional
    public List<UserRoleRow> getRolesByUserId(Long userId) {
        String sql = """
                SELECT
                    r.CODE AS role_code,
                    r.NAME AS role_name
                FROM AUTH_USER_ROLE ur
                JOIN AUTH_ROLE r ON r.ID = ur.ROLE_ID
                WHERE ur.USER_ID = :userId
                  AND ur.STATUS = 'ACTIVE'
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();

        return rows.stream()
                .map(r -> UserRoleRow.builder()
                        .roleCode((String) r[0])
                        .roleName((String) r[1])
                        .build())
                .collect(Collectors.toList());
    }

    // ── Native SQL: tìm kiếm resource (thay thế procedure DBMS_SQL/TO_REFCURSOR) ──

    @Transactional
    public List<ResourceRow> getResource(Long appId, Long resourceId, String resourceCode, String serviceCode) {
        StringBuilder sql = new StringBuilder(
                "SELECT r.ID, a.ID, r.RESOURCE_CODE, r.RESOURCE_NAME, r.RESOURCE_TYPE, "
                + "r.ACTIONS, r.LDAP_GROUP_NAME, r.DESCRIPTION, r.STATUS, "
                + "a.NAME, a.APP_TYPE, a.SERVICE_CODE, a.STATUS "
                + "FROM AUTH_RESOURCE r JOIN AUTH_APPLICATION a ON a.ID = r.APP_ID WHERE 1=1");

        if (resourceId != null)   sql.append(" AND r.ID = :resourceId");
        if (appId != null)        sql.append(" AND a.ID = :appId");
        if (resourceCode != null) sql.append(" AND r.RESOURCE_CODE = :resourceCode");
        if (serviceCode != null)  sql.append(" AND a.SERVICE_CODE = :serviceCode");

        Query nq = entityManager.createNativeQuery(sql.toString());
        if (resourceId != null)   nq.setParameter("resourceId", resourceId);
        if (appId != null)        nq.setParameter("appId", appId);
        if (resourceCode != null) nq.setParameter("resourceCode", resourceCode);
        if (serviceCode != null)  nq.setParameter("serviceCode", serviceCode);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nq.getResultList();

        return rows.stream().map(r -> ResourceRow.builder()
                .resourceId(toLong(r[0]))
                .appId(toLong(r[1]))
                .resourceCode((String) r[2])
                .resourceName((String) r[3])
                .resourceType((String) r[4])
                .actions((String) r[5])
                .ldapGroupName((String) r[6])
                .description((String) r[7])
                .resourceStatus((String) r[8])
                .appName((String) r[9])
                .appType((String) r[10])
                .serviceCode((String) r[11])
                .appStatus((String) r[12])
                .build())
                .collect(Collectors.toList());
    }

    // ── Native SQL: danh sách user (dynamic filter + pagination) ─────────────

    @Transactional
    public List<Object[]> getUsersList(String username, String employeeCode,
            Long departmentId, String status, long offset, int size, Boolean onLeave, Boolean offboarded) {

        String where = buildUserWhere(username, employeeCode, departmentId, status, onLeave, offboarded);
        String sql = "SELECT u.ID, u.USERNAME, u.EMAIL, u.MOBILE, u.STATUS, "
                + "up.EMPLOYEE_CODE, up.FULL_NAME, up.POSITION, up.JOIN_DATE, up.DEPARTMENT_ID "
                + "FROM AUTH_USER u JOIN AUTH_USER_PROFILE up ON up.USER_ID = u.ID "
                + where
                + " ORDER BY u.USERNAME ASC"
                + " OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY";

        Query nq = entityManager.createNativeQuery(sql);
        bindUserParams(nq, username, employeeCode, departmentId, status);
        nq.setParameter("offset", offset);
        nq.setParameter("size", size);

        @SuppressWarnings("unchecked")
        List<Object[]> result = nq.getResultList();
        return result;
    }

    @Transactional
    public long countUsers(String username, String employeeCode,
            Long departmentId, String status, Boolean onLeave, Boolean offboarded) {

        String where = buildUserWhere(username, employeeCode, departmentId, status, onLeave, offboarded);
        String sql = "SELECT COUNT(*) FROM AUTH_USER u "
                + "JOIN AUTH_USER_PROFILE up ON up.USER_ID = u.ID " + where;

        Query nq = entityManager.createNativeQuery(sql);
        bindUserParams(nq, username, employeeCode, departmentId, status);
        return ((Number) nq.getSingleResult()).longValue();
    }

    private String buildUserWhere(String username, String employeeCode,
            Long departmentId, String status, Boolean onLeave, Boolean offboarded) {

        StringBuilder w = new StringBuilder("WHERE 1=1");
        if (username != null && !username.isBlank()) {
            w.append(" AND u.USERNAME LIKE :username");
        }
        if (employeeCode != null && !employeeCode.isBlank()) {
            w.append(" AND up.EMPLOYEE_CODE LIKE :employeeCode");
        }
        if (departmentId != null) {
            w.append(" AND up.DEPARTMENT_ID = :departmentId");
        }
        if (Boolean.TRUE.equals(onLeave)) {
            // User tạm hoãn công tác: INACTIVE + còn SUSPENDED permission
            w.append(" AND u.STATUS = 'INACTIVE'");
            w.append(" AND EXISTS (SELECT 1 FROM AUTH_APP_PERMISSION ap2"
                    + " WHERE ap2.USER_ID = u.ID AND ap2.STATUS = 'SUSPENDED')");
        } else if (Boolean.TRUE.equals(offboarded)) {
            // User đã nghỉ việc hẳn: INACTIVE + không còn SUSPENDED permission nào
            w.append(" AND u.STATUS = 'INACTIVE'");
            w.append(" AND NOT EXISTS (SELECT 1 FROM AUTH_APP_PERMISSION ap2"
                    + " WHERE ap2.USER_ID = u.ID AND ap2.STATUS = 'SUSPENDED')");
        } else if (status != null) {
            w.append(" AND u.STATUS = :status");
        } else {
            w.append(" AND u.STATUS != 'DELETED'");
        }
        return w.toString();
    }

    private void bindUserParams(Query nq, String username, String employeeCode,
            Long departmentId, String status) {

        if (username != null && !username.isBlank()) {
            nq.setParameter("username", username + "%");
        }
        if (employeeCode != null && !employeeCode.isBlank()) {
            nq.setParameter("employeeCode", employeeCode + "%");
        }
        if (departmentId != null) {
            nq.setParameter("departmentId", departmentId);
        }
        if (status != null) {
            nq.setParameter("status", status);
        }
    }

    // Lấy danh sách có filter + pagination + sort direction
    @Transactional
    public List<PermissionRequest> getPermissionRequests(String status, String requester, String reviewer,
            LocalDate from, LocalDate to, long offset, int size, String sortDir, String currentUserCode) {

        String direction = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        StringBuilder sql = new StringBuilder(
                "SELECT h.ID, h.STATUS, h.REASON, h.NOTE, "
                        + "h.REQUESTED_BY, h.REVIEWED_BY, h.REQUESTED_AT, h.REVIEWED_AT, "
                        + "u1.USERNAME AS REQUESTER_USERNAME, up1.FULL_NAME AS REQUESTER_FULL_NAME, "
                        + "u2.USERNAME AS REVIEWER_USERNAME, up2.FULL_NAME AS REVIEWER_FULL_NAME, "
                        + "h.REQUEST_FOR AS GRANTEE_CODE, "
                        + "u3.USERNAME AS GRANTEE_USERNAME, up3.FULL_NAME AS GRANTEE_FULL_NAME "
                        + "FROM AUTH_REQUEST_HEADER h "
                        + "LEFT JOIN AUTH_USER_PROFILE up1 ON up1.EMPLOYEE_CODE = h.REQUESTED_BY "
                        + "LEFT JOIN AUTH_USER u1 ON u1.ID = up1.USER_ID "
                        + "LEFT JOIN AUTH_USER_PROFILE up2 ON up2.EMPLOYEE_CODE = h.REVIEWED_BY "
                        + "LEFT JOIN AUTH_USER u2 ON u2.ID = up2.USER_ID "
                        + "LEFT JOIN AUTH_USER_PROFILE up3 ON up3.EMPLOYEE_CODE = h.REQUEST_FOR "
                        + "LEFT JOIN AUTH_USER u3 ON u3.ID = up3.USER_ID "
                        + "WHERE (h.REQUESTED_BY = :currentUserCode OR h.REVIEWED_BY = :currentUserCode)");

        if (!ObjectUtils.isEmpty(status))
            sql.append(" AND h.STATUS = :status");
        if (!ObjectUtils.isEmpty(requester))
            sql.append(" AND h.REQUESTED_BY = :requester");
        if (!ObjectUtils.isEmpty(reviewer))
            sql.append(" AND h.REVIEWED_BY = :reviewer");
        if (from != null)
            sql.append(" AND h.REQUESTED_AT >= :from");
        if (to != null)
            sql.append(" AND h.REQUESTED_AT <= :to");

        sql.append(" ORDER BY h.REQUESTED_AT ").append(direction);
        sql.append(" OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY");

        Query nq = entityManager.createNativeQuery(sql.toString());

        nq.setParameter("currentUserCode", currentUserCode);
        if (!ObjectUtils.isEmpty(status))
            nq.setParameter("status", status);
        if (!ObjectUtils.isEmpty(requester))
            nq.setParameter("requester", requester);
        if (!ObjectUtils.isEmpty(reviewer))
            nq.setParameter("reviewer", reviewer);
        if (from != null)
            nq.setParameter("from", from.atStartOfDay());
        if (to != null)
            nq.setParameter("to", to.atTime(23, 59, 59));
        nq.setParameter("offset", offset);
        nq.setParameter("size", size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nq.getResultList();

        return rows.stream().map((Object[] row) -> PermissionRequest.builder()
                .requestId(toLong(row[0]))
                .status((String) row[1])
                .reason((String) row[2])
                .note((String) row[3])
                .requesterCode((String) row[4])
                .reviewerCode((String) row[5])
                .requestedAt(toLocalDateTime(row[6]))
                .reviewedAt(toLocalDateTime(row[7]))
                .requesterUsername((String) row[8])
                .requesterFullName((String) row[9])
                .reviewerUsername((String) row[10])
                .reviewerFullName((String) row[11])
                .granteeCode((String) row[12])
                .granteeUsername((String) row[13])
                .granteeFullName((String) row[14])
                .build())
                .collect(Collectors.toList());
    }

    // Đếm tổng để build Page
    @Transactional
    public long countPermissionRequests(String status, String requester,
            String reviewer, LocalDate from, LocalDate to, String currentUserCode) {

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM AUTH_REQUEST_HEADER h "
                + "WHERE (h.REQUESTED_BY = :currentUserCode OR h.REVIEWED_BY = :currentUserCode)");

        if (!ObjectUtils.isEmpty(status))
            sql.append(" AND h.STATUS = :status");
        if (!ObjectUtils.isEmpty(requester))
            sql.append(" AND h.REQUESTED_BY = :requester");
        if (!ObjectUtils.isEmpty(reviewer))
            sql.append(" AND h.REVIEWED_BY = :reviewer");
        if (from != null)
            sql.append(" AND h.REQUESTED_AT >= :from");
        if (to != null)
            sql.append(" AND h.REQUESTED_AT <= :to");

        Query nq = entityManager.createNativeQuery(sql.toString());

        nq.setParameter("currentUserCode", currentUserCode);
        if (!ObjectUtils.isEmpty(status))
            nq.setParameter("status", status);
        if (!ObjectUtils.isEmpty(requester))
            nq.setParameter("requester", requester);
        if (!ObjectUtils.isEmpty(reviewer))
            nq.setParameter("reviewer", reviewer);
        if (from != null)
            nq.setParameter("from", from.atStartOfDay());
        if (to != null)
            nq.setParameter("to", to.atTime(23, 59, 59));

        return ((Number) nq.getSingleResult()).longValue();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long toLong(Object val) {
        return val != null ? ((Number) val).longValue() : null;
    }

    private LocalDate toLocalDate(Object val) {
        if (val == null)
            return null;
        if (val instanceof java.sql.Date d)
            return d.toLocalDate();
        if (val instanceof java.sql.Timestamp ts)
            return ts.toLocalDateTime().toLocalDate();
        return null;
    }

    private java.time.LocalDateTime toLocalDateTime(Object val) {
        if (val == null)
            return null;
        if (val instanceof java.sql.Timestamp ts)
            return ts.toLocalDateTime();
        if (val instanceof LocalDateTime ts) {
            return ts;
        }
        return null;
    }
}
