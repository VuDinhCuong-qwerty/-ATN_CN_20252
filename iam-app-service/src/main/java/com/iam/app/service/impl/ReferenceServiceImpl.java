package com.iam.app.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.iam.app.domain.AuthDepartment;
import com.iam.app.domain.AuthPosition;
import com.iam.app.domain.AuthRole;
import com.iam.app.dto.response.GetDepartmentResponse;
import com.iam.app.dto.response.GetPositionsResponse;
import com.iam.app.dto.response.GetRolesResponse;
import com.iam.app.repository.jpa.AuthDepartmentRepository;
import com.iam.app.repository.jpa.AuthPositionRepository;
import com.iam.app.repository.jpa.AuthRoleRepository;
import com.iam.app.service.ReferenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceServiceImpl implements ReferenceService {

    private final AuthRoleRepository roleRepository;
    private final AuthPositionRepository positionRepository;
    private final AuthDepartmentRepository departmentRepository;

    @Override
    public List<GetRolesResponse> getRoles() {

        List<AuthRole> roles = this.roleRepository.findAllRoles();
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        List<GetRolesResponse> output = roles.stream().map(item -> new GetRolesResponse(item))
                .collect(Collectors.toList());
        return output;
    }

    @Override
    public List<GetPositionsResponse> getPositions(String status) {
        if (status == null || status.isBlank())
            status = null;
        List<AuthPosition> positions = this.positionRepository.findByStatus(status);
        if (positions == null || positions.isEmpty()) {
            return List.of();
        }
        List<GetPositionsResponse> output = positions.stream().map(item -> new GetPositionsResponse(item))
                .collect(Collectors.toList());
        return output;
    }

    @Override
    public List<GetDepartmentResponse> getDepartments(Long status) {
        List<AuthDepartment> authDepartments = this.departmentRepository.findByStatus(status);
        if (authDepartments == null || authDepartments.isEmpty()) {
            return List.of();
        }

        List<GetDepartmentResponse> departments = authDepartments.stream()
                .map(GetDepartmentResponse::new)
                .collect(Collectors.toList());

        Map<Long, GetDepartmentResponse> departmentMap = departments.stream()
                .collect(Collectors.toMap(GetDepartmentResponse::getId, item -> item));

        List<GetDepartmentResponse> roots = new ArrayList<>();
        for (GetDepartmentResponse dept : departments) {
            Long parentId = dept.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(dept);
            } else {
                GetDepartmentResponse parent = departmentMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(dept);
                }
            }
        }
        return roots;
    }

}
