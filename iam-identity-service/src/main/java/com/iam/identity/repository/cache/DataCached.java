package com.iam.identity.repository.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.iam.identity.domain.AuthDepartment;
import com.iam.identity.repository.jpa.AuthDepartmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataCached {

    private final AuthDepartmentRepository departmentRepository;

    private final Cache<Long, String> departmentCached = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(500)
            .recordStats()
            .build();

    public String getDetailDepartment(Long departmentId) {
        String cached = departmentCached.getIfPresent(departmentId);
        if (cached != null) return cached;

        Map<Long, String> allPaths = buildAllPaths(departmentId);
        departmentCached.putAll(allPaths);
        return allPaths.getOrDefault(departmentId, "");
    }

    private Map<Long, String> buildAllPaths(Long departmentId) {
        Map<Long, String> result = new HashMap<>();
        List<AuthDepartment> departments = departmentRepository.findFullTreeDepartmentById(departmentId);
        Map<Long, AuthDepartment> departmentMap = new HashMap<>();

        for (AuthDepartment dept : departments) {
            departmentMap.put(dept.getId(), dept);
        }

        for (AuthDepartment department : departments) {
            List<String> names = new ArrayList<>();
            Long current = department.getId();
            int guard = 0;

            while (current != null && current != 0 && guard < 5) {
                AuthDepartment dept = departmentMap.get(current);
                if (dept == null) break;
                names.add(dept.getName());
                current = dept.getParentId();
                guard++;
            }
            if (!names.isEmpty()) {
                result.put(department.getId(), String.join(", ", names));
            }
        }
        return result;
    }
}
