package com.iam.app.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.iam.app.dto.pojo.AppInfor;
import com.iam.app.dto.pojo.Department;
import com.iam.app.dto.response.ClientListResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRepository {

    private final EntityManager entityManager;

    public AppInfor getAppInforById(Long id) {
        String sql = """
                SELECT
                    a.ID, a.NAME, a.DESCRIPTION, a.APP_TYPE,
                    a.LOGO_URI, a.DEFAULT_URL, a.STATUS,
                    a.DEPARTMENT_ID, a.CREATED_AT, a.UPDATED_AT,
                    a.SERVICE_CODE, a.GROUP_ID, a.ACR_LEVEL,
                    g.ID, g.NAME, g.DESCRIPTION
                FROM AUTH_APPLICATION a
                LEFT JOIN AUTH_CLIENT_GROUP g ON g.ID = a.GROUP_ID
                WHERE a.ID = :id
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .getResultList();
        if (rows.isEmpty()) return null;

        Object[] r = rows.get(0);
        return AppInfor.builder()
                .id(toLong(r[0]))
                .name((String) r[1])
                .description((String) r[2])
                .appType((String) r[3])
                .logoUri((String) r[4])
                .defaultUrl((String) r[5])
                .status((String) r[6])
                .departmentId(toLong(r[7]))
                .createdAt(toLocalDateTime(r[8]))
                .updatedAt(toLocalDateTime(r[9]))
                .serviceCode((String) r[10])
                .groupId(toLong(r[11]))
                .acrLevel(toLong(r[12]))
                .groupName((String) r[14])
                .build();
    }

    public Department getDepartmentById(Long id) {
        String sql = """
                SELECT d.ID, d.CODE, d.NAME, d.PARENT_ID, LEVEL
                FROM AUTH_DEPARTMENT d
                WHERE d.STATUS = 1
                START WITH d.ID = :id
                CONNECT BY PRIOR d.PARENT_ID = d.ID AND LEVEL <= 5
                ORDER BY LEVEL
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .getResultList();
        if (rows.isEmpty()) return null;

        // rows[0] = department hiện tại (LEVEL 1) con -> cha, ....
        List<Department> nodes = rows.stream()
                .map(r -> Department.builder()
                        .id(toLong(r[0]))
                        .code((String) r[1])
                        .name((String) r[2])
                        .parentId(toLong(r[3]))
                        .build())
                .collect(Collectors.toList());
        String detail = nodes.stream()
                .map(Department::getName)
                .collect(Collectors.joining(", "));

        Department current = nodes.get(0);
        current.setDetail(detail);
        return current;
    }

    public Page<ClientListResponse> getClients(
            String grantType, Long appId, Integer enabled, String clientId, Pageable pageable) {

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (grantType != null) where.append(" AND LOWER(o.GRANT_TYPES) LIKE LOWER(:grantType)");
        if (appId    != null)  where.append(" AND c.APP_ID = :appId");
        if (enabled  != null)  where.append(" AND c.ENABLED = :enabled");
        if (clientId != null)  where.append(" AND LOWER(c.CLIENT_ID) LIKE LOWER(:clientId)");

        String countSql = "SELECT COUNT(1) FROM AUTH_CLIENT c " +
                          "LEFT JOIN AUTH_CLIENT_OAUTH o ON o.CLIENT_ID = c.ID" + where;

        String dataSql  = "SELECT c.ID, c.CLIENT_ID, c.NAME, c.CLIENT_TYPE, c.ENABLED, " +
                          "c.APP_ID, c.CREATED_AT, o.GRANT_TYPES, a.NAME " +
                          "FROM AUTH_CLIENT c " +
                          "LEFT JOIN AUTH_CLIENT_OAUTH o ON o.CLIENT_ID = c.ID " +
                          "LEFT JOIN AUTH_APPLICATION a ON a.ID = c.APP_ID" + where +
                          " ORDER BY c.CREATED_AT DESC" +
                          " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";

        Query countQuery = entityManager.createNativeQuery(countSql);
        Query dataQuery  = entityManager.createNativeQuery(dataSql);

        applyFilters(countQuery, dataQuery, grantType, appId, enabled, clientId);
        dataQuery.setParameter("offset",   pageable.getOffset());
        dataQuery.setParameter("pageSize", pageable.getPageSize());

        long total = ((Number) countQuery.getSingleResult()).longValue();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<ClientListResponse> content = rows.stream()
                .map(ClientListResponse::new)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    private void applyFilters(Query countQuery, Query dataQuery,
            String grantType, Long appId, Integer enabled, String clientId) {
        if (grantType != null) {
            String p = "%" + grantType + "%";
            countQuery.setParameter("grantType", p);
            dataQuery.setParameter("grantType",  p);
        }
        if (appId != null) {
            countQuery.setParameter("appId", appId);
            dataQuery.setParameter("appId",  appId);
        }
        if (enabled != null) {
            countQuery.setParameter("enabled", enabled);
            dataQuery.setParameter("enabled",  enabled);
        }
        if (clientId != null) {
            String p = "%" + clientId + "%";
            countQuery.setParameter("clientId", p);
            dataQuery.setParameter("clientId",  p);
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
