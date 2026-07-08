package com.iam.ldap.service;

import com.iam.ldap.model.UserEntry;
import com.iam.ldap.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Value("${ldap.server.suffix:dc=iam,dc=bank,dc=vn}")
    private String suffix;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tìm user theo username.
     * Dùng cho BIND (OracleAuthenticator) và LOOKUP với old-style DN (uid=X,ou=users,...).
     */
    public Optional<UserEntry> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Lấy toàn bộ user ACTIVE.
     * Dùng cho SEARCH khi base là ou=users,dc=... (không có service filter).
     */
    public List<UserEntry> findAll() {
        return userRepository.findAll();
    }

    /**
     * Lấy toàn bộ user ACTIVE có quyền truy cập service.
     * Dùng cho SEARCH khi base DN chứa service OU:
     *   base = ou=gitlab-server,ou=users,dc=... → serviceCode = "gitlab-server"
     */
    public List<UserEntry> findAllByService(String serviceCode) {
        return userRepository.findAllByService(serviceCode);
    }

    /**
     * Tìm user theo username VÀ kiểm tra quyền truy cập service cùng lúc.
     * Dùng cho LOOKUP và hasEntry với service-encoded DN:
     *   uid=CUONGVD,ou=gitlab-server,ou=users,...
     */
    public Optional<UserEntry> findByUsernameAndService(String username, String serviceCode) {
        return userRepository.findByUsernameAndService(username, serviceCode);
    }

    /**
     * Kiểm tra user có quyền truy cập app theo serviceCode không.
     * Dùng trong OracleAuthenticator sau khi BCrypt verify thành công.
     */
    public boolean hasAppPermission(String username, String serviceCode) {
        return userRepository.hasAppPermission(username, serviceCode);
    }

    /**
     * Bổ sung memberOf vào UserEntry từ bảng AUTH_APP_PERMISSION.
     * Format memberOf DN: cn={serviceCode},ou=groups,{suffix}
     */
    public UserEntry enrichWithMemberOf(UserEntry user) {
        // 1. Lấy danh sách service code mà user có quyền
        List<String> codes = userRepository.findAppCodesByUserId(user.getId());
        // 2. Build DN cho mỗi service
        List<String> memberOf = new ArrayList<>();
        for (String code : codes) {
            memberOf.add("cn=" + code + ",ou=groups," + suffix);
        }
        user.setMemberOf(memberOf);
        return user;
    }

    /**
     * Bổ sung permissions vào UserEntry từ AUTH_USER_RESOURCE.
     * Format: "serviceCode/resourceCode:action"  vd: "kibana-server/iam-system-logs:view"
     * Dùng cho LDAP attribute 'description' → ES metadata.description → role mapping.
     */
    public UserEntry enrichWithPermissions(UserEntry user) {
        List<String> permissions = userRepository.findPermissionsByUserId(user.getId());
        user.setPermissions(permissions);
        return user;
    }
}
