package com.iam.auth.repository.jpa;

import com.iam.auth.dto.pojo.Client;
import com.iam.auth.dto.pojo.UserAppPermission;
import com.iam.auth.dto.pojo.UserPermissionRow;
import com.iam.auth.dto.response.GetUserInfo;
import com.iam.auth.engine.FlowNode;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.dialect.OracleTypes;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class AuthRepository {

    private final EntityManager entityManager;

    public AuthRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<FlowNode> getNodeByFlowId(Long id) {
        Session session = this.entityManager.unwrap(Session.class);
//        return session.doReturningWork(connection -> mapFlowNodes(connection, id));
        return session.doReturningWork(connection -> {
            List<FlowNode> result = new ArrayList<>();

            try (CallableStatement stmt = connection.prepareCall("{call AUTH_PKG.get_flow_by_id(?, ?)}")) {
                stmt.setLong("p_flow_id", id);
                stmt.registerOutParameter("p_result", OracleTypes.CURSOR);
                stmt.execute();

                try (ResultSet rs = (ResultSet) stmt.getObject("p_result")) {
                    while (rs.next()) {
                        FlowNode node = FlowNode.builder()
                                .executionId(rs.getLong("node_id"))
                                .appId(rs.getLong("app_id"))
                                .clientMethodId(rs.getLong("client_method_id"))
                                .method(rs.getString("method"))
                                .requirement(rs.getString("requirement"))
                                .arcLevel(rs.getLong("app_level"))
                                .isDefault(rs.getLong("is_default") == 1)
                                .parentNodeId(
                                        (rs.getObject("parent_node_id")) == null ? null : rs.getLong("parent_node_id")
                                )
                                .theme(rs.getString("theme"))
                                .children(new ArrayList<>())
                                .build();
                        result.add(node);
                    }
                }
            }
            return result;
        });
    }

    @Transactional
    public Client getClientByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) return null;

        Session session = this.entityManager.unwrap(Session.class);
        return session.doReturningWork(connection -> {
            List<Client> clients = new ArrayList<>();
            try (CallableStatement stm = connection.prepareCall("{call AUTH_PKG.get_client_by_client_id(?, ?)}")) {
                stm.setString("p_client_id", clientId);
                stm.registerOutParameter("p_result", OracleTypes.CURSOR);
                stm.execute();

                try (ResultSet rs = (ResultSet) stm.getObject("p_result")) {
                    while (rs.next()) {
                        Client client = Client.builder()
                                .id(rs.getLong("id"))
                                .clientId(rs.getString("client_id"))
                                .clientSecret(rs.getString("client_secret"))
                                .name(rs.getString("name"))
                                .clientType(rs.getString("client_type"))
                                .enabled(rs.getInt("enabled") == 1)
                                .appId(rs.getLong("app_id"))
                                .grantTypes(splitToList(rs.getString("grant_types"), ","))
                                .redirectUris(splitToList(rs.getString("redirect_uris"), ","))
                                .allowedScopes(splitToList(rs.getString("allowed_scopes"), "\\s+"))
                                .accessTokenTTL(rs.getLong("access_token_ttl"))
                                .refreshTokenTTL(rs.getLong("refresh_token_ttl"))
                                .idTokenTTL(rs.getLong("id_token_ttl"))
                                .tokenEndpointAuth(splitToList(rs.getString("token_endpoint_auth"), ","))
                                .requiredPKCE(rs.getInt("require_pkce") == 1)
                                .requiredConsent(rs.getInt("require_consent") == 1)
                                .postLogoutRedirect(rs.getString("post_logout_redirect"))
                                .defaultUrl(rs.getString("default_url"))
                                .build();
                        clients.add(client);
                    }
                }
            }
            if (clients.isEmpty()) {
                return null;
            }
            Long id = clients.getFirst().getId();
            for (Client client: clients) {
                if (!Objects.equals(id, client.getId())) {
                    return null;
                }
            }
            return clients.getFirst();
        });
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public UserAppPermission getUserAppPermit(Long userID, Long clientId) {
        String sql = "SELECT " +
                "    u.id as user_id, " +
                "    u.username as username, " +
                "    a.id as application_id, " +
                "    a.name as application_name, " +
                "    a.app_type as application_type, " +
                "    c.id as client_id, " +
                "    c.enabled as client_status, " +
                "    r.code as role " +
                "FROM auth_user u " +
                "JOIN auth_app_permission ap " +
                "    ON u.id = ap.user_id " +
                "JOIN auth_application a " +
                "    ON a.id = ap.app_id " +
                "JOIN auth_client c " +
                "    ON c.app_id = a.id " +
                "JOIN auth_user_role ur " +
                "    ON ur.user_id = u.id " +
                "JOIN auth_role r " +
                "    ON r.id = ur.role_id " +
                "WHERE u.id = :userId AND (c.id is null or c.id = :clientId) AND ap.status = 'ACTIVE'";
        List<Object[]> rs = entityManager.createNativeQuery(sql)
                .setParameter("userId", userID)
                .setParameter("clientId", clientId)
                .getResultList();

        if (rs.isEmpty()) return null;
        List<UserAppPermission> result = rs.stream().map(
                r -> {
                    Object[] row = (Object[]) r;
                    return UserAppPermission.builder()
                            .userId(getLong(row[0]))
                            .username((String) row[1])
                            .applicationId(getLong(row[0]))
                            .applicationName((String) row[3])
                            .applicationType((String) row[4])
                            .clientId(getLong(row[0]))
                            .clientStatus((getLong(row[0])) == 1)
                            .role((String) row[7])
                            .build();
                }
        ).toList();
        return result.getFirst();
    }

    @Transactional
    public List<UserPermissionRow> getUserPermission(Long userId, Long appId) {
        Session session = this.entityManager.unwrap(Session.class);
        return session.doReturningWork(connection -> {
            List<UserPermissionRow> result = new ArrayList<>();
            try (CallableStatement stmt = connection.prepareCall("{call AUTH_PKG.get_permission(?, ?, ?)}")) {
                stmt.setLong("p_user_id", userId);
                stmt.setLong("p_app_id", appId);
                stmt.registerOutParameter("p_result", OracleTypes.CURSOR);
                stmt.execute();

                try (ResultSet rs = (ResultSet) stmt.getObject("p_result")) {
                    while (rs.next()) {
                        result.add(UserPermissionRow.builder()
                                .userId(rs.getLong("user_id"))
                                .username(rs.getString("username"))
                                .displayName(rs.getString("display_name"))
                                .mobile(rs.getString("mobile"))
                                .userActions(splitToList(rs.getString("user_action"), ","))
                                .resourceCode(rs.getString("resource_code"))
                                .resourceActions(splitToList(rs.getString("resource_actions"), ","))
                                .appId(rs.getLong("app_id"))
                                .role(rs.getString("role"))
                                .build());
                    }
                }
            }
            return result;
        });
    }

    @Transactional
    public GetUserInfo getUserInfoByUserId(Long userId) {
        if (userId == null) return null;

        Session session = this.entityManager.unwrap(Session.class);
        return session.doReturningWork(connection -> {
            try (CallableStatement stmt = connection.prepareCall(
                    "{call AUTH_PKG.get_user_info_by_user_id(?, ?, ?, ?, ?)}")) {

                stmt.setLong("p_user_id", userId);
                stmt.registerOutParameter("p_user_profile", OracleTypes.CURSOR);
                stmt.registerOutParameter("p_user_department", OracleTypes.CURSOR);
                stmt.registerOutParameter("p_user_address", OracleTypes.CURSOR);
                stmt.registerOutParameter("p_user_role", OracleTypes.CURSOR);
                stmt.execute();

                GetUserInfo userInfo;

                // Step 1: map user profile — must be exactly 1 row
                try (ResultSet rs = (ResultSet) stmt.getObject("p_user_profile")) {
                    if (!rs.next()) return null;

                    userInfo = GetUserInfo.builder()
                            .email(rs.getString("email"))
                            .mobile(rs.getString("mobile"))
                            .status(rs.getString("status"))
                            .employeeCode(rs.getString("employee_code"))
                            .fullName(rs.getString("full_name"))
                            .firstName(rs.getString("first_name"))
                            .lastName(rs.getString("last_name"))
                            .displayName(rs.getString("display_name"))
                            .gender(rs.getString("gender"))
                            .dob(rs.getTimestamp("dob") != null ? rs.getTimestamp("dob").toInstant() : null)
                            .nationality(rs.getString("nationality"))
                            .ethnic(rs.getString("ethnic"))
                            .religion(rs.getString("religion"))
                            .avatarUrl(rs.getString("avatar_url"))
                            .numberId(rs.getString("cccd"))
                            .numberIdIssueDate(rs.getDate("cccd_issued_date") != null
                                    ? rs.getDate("cccd_issued_date").toLocalDate() : null)
                            .numberIdIssuePlace(rs.getString("cccd_issued_place"))
                            .joinDate(rs.getDate("join_date") != null
                                    ? rs.getDate("join_date").toLocalDate() : null)
                            .leaveDate(rs.getDate("leave_date") != null
                                    ? rs.getDate("leave_date").toLocalDate() : null)
                            .position(rs.getString("position"))
                            .build();

                    if (rs.next()) {
                        log.warn("getUserInfoByUserId: multiple profile rows for userId={}", userId);
                        return null;
                    }
                }

                // Step 2: map department
                // rows are ordered child → ancestor (up to 5 levels)
                // id/code/name come from the first (deepest) row
                // details = "child, parent, grandparent, ..." joined in order
                try (ResultSet rs = (ResultSet) stmt.getObject("p_user_department")) {
                    Long deptId = null;
                    String deptCode = null;
                    String deptName = null;
                    List<String> nameChain = new ArrayList<>();

                    while (rs.next()) {
                        if (deptId == null) {
                            deptId = rs.getLong("department_id");
                            deptCode = rs.getString("code");
                            deptName = rs.getString("name");
                        }
                        nameChain.add(rs.getString("name"));
                    }

                    if (deptId != null) {
                        userInfo.setDepartment(userInfo.new Department(
                                deptId,
                                deptCode,
                                deptName,
                                String.join(", ", nameChain)
                        ));
                    }
                }

                // Step 3: map addresses by type — max 3 rows expected (PERMANENT/TEMPORARY/BIRTH_PLACE)
                try (ResultSet rs = (ResultSet) stmt.getObject("p_user_address")) {
                    int addressCount = 0;
                    while (rs.next()) {
                        addressCount++;
                        if (addressCount > 3) {
                            log.warn("getUserInfoByUserId: more than 3 address rows for userId={}", userId);
                            return null;
                        }
                        String type = rs.getString("type");
                        Long provinceCode = rs.getObject("province_code") != null ? rs.getLong("province_code") : null;
                        Long wardCode = rs.getObject("ward_code") != null ? rs.getLong("ward_code") : null;
                        GetUserInfo.Address addr = userInfo.new Address(
                                provinceCode, rs.getString("province_name"),
                                wardCode, rs.getString("ward_name"),
                                rs.getString("detail")
                        );
                        if ("PERMANENT".equalsIgnoreCase(type)) {
                            userInfo.setPermanentAddress(addr);
                        } else if ("TEMPORARY".equalsIgnoreCase(type)) {
                            userInfo.setTemporaryAddress(addr);
                        } else if ("BIRTH_PLACE".equalsIgnoreCase(type)) {
                            userInfo.setBirthPlace(addr);
                        }
                    }
                }
                if (userInfo.getBirthPlace() == null) {
                    log.warn("getUserInfoByUserId: missing BIRTH_PLACE address for userId={}", userId);
                    return null;
                }

                // Step 4: map roles — at least 1 role required
                List<GetUserInfo.Role> roles = new ArrayList<>();
                try (ResultSet rs = (ResultSet) stmt.getObject("p_user_role")) {
                    while (rs.next()) {
                        roles.add(userInfo.new Role(
                                rs.getLong("role_id"),
                                rs.getString("code"),
                                rs.getString("name")
                        ));
                    }
                }
                if (roles.isEmpty()) {
                    log.warn("getUserInfoByUserId: no active role found for userId={}", userId);
                    return null;
                }
                userInfo.setRoles(roles);

                return userInfo;
            }
        });
    }

    private List<String> splitToList(String value, String split) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(split))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Long getLong(Object val) {
        return val == null ? null : ((Number) val).longValue();
    }
}