package com.iam.ldap.service;

import com.iam.ldap.model.ApplicationEntry;
import com.iam.ldap.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Value("${ldap.server.suffix:dc=iam,dc=bank,dc=vn}")
    private String suffix;

    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    /**
     * Lấy toàn bộ application ACTIVE, kèm danh sách member DN.
     * Dùng cho SEARCH ou=groups (OraclePartition.searchGroups).
     */
    public List<ApplicationEntry> findAll() {
        List<ApplicationEntry> apps = applicationRepository.findAll();
        List<ApplicationEntry> result = new ArrayList<>();
        for (ApplicationEntry app : apps) {
            enrichWithMembers(app);
            result.add(app);
        }
        return result;
    }

    /**
     * Tìm application theo serviceCode, kèm danh sách member DN.
     * Dùng cho LOOKUP cn=serviceCode (OraclePartition.lookup).
     */
    public Optional<ApplicationEntry> findByServiceCode(String serviceCode) {
        Optional<ApplicationEntry> appOpt = applicationRepository.findByServiceCode(serviceCode);
        if (appOpt.isEmpty()) {
            return Optional.empty();
        }
        ApplicationEntry app = appOpt.get();
        enrichWithMembers(app);
        return Optional.of(app);
    }

    /**
     * Kiểm tra app có tồn tại và ACTIVE theo serviceCode — không load full entry.
     * Dùng cho hasEntry() khi gặp virtual service OU: ou=gitlab-server,ou=users,...
     */
    public boolean existsByServiceCode(String serviceCode) {
        return applicationRepository.existsByServiceCode(serviceCode);
    }

    /**
     * Tìm các application mà user có quyền truy cập (AUTH_APP_PERMISSION ACTIVE).
     * Dùng khi GitLab search groups với filter (member=uid=USERNAME,...).
     */
    public List<ApplicationEntry> findByMember(String username) {
        List<ApplicationEntry> apps = applicationRepository.findByMember(username);
        List<ApplicationEntry> result = new ArrayList<>();
        for (ApplicationEntry app : apps) {
            enrichWithMembers(app);
            result.add(app);
        }
        return result;
    }

    /**
     * Bổ sung danh sách member vào ApplicationEntry.
     * Member DN dùng service-encoded format:
     *   uid={username},ou={serviceCode},ou=users,{suffix}
     * Ngữ nghĩa: user này là member của group SERVICE vì họ có permission cho SERVICE.
     */
    private void enrichWithMembers(ApplicationEntry app) {
        // 1. Lấy danh sách username có quyền trong app này
        List<String> usernames = applicationRepository.findUsernamesByAppId(app.getId());
        // 2. Build DN với service code được encode vào path
        List<String> members = new ArrayList<>();
        for (String username : usernames) {
            members.add("uid=" + username + ",ou=" + app.getServiceCode() + ",ou=users," + suffix);
        }
        app.setMembers(members);
    }
}
