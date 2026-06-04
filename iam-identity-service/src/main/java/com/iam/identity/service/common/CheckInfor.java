package com.iam.identity.service.common;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.iam.identity.domain.AuthDepartment;
import com.iam.identity.domain.AuthPosition;
import com.iam.identity.domain.AuthRole;
import com.iam.identity.dto.pojo.DuplicateUserrData;
import com.iam.identity.dto.pojo.Place;
import com.iam.identity.dto.request.CreateUserRequest.Role;
import com.iam.identity.enums.ErrorCode;
import com.iam.identity.exception.BusinessException;
import com.iam.identity.repository.jpa.AuthDepartmentRepository;
import com.iam.identity.repository.jpa.AuthPositionRepository;
import com.iam.identity.repository.jpa.AuthRepository;
import com.iam.identity.repository.jpa.AuthRoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInfor {

    private final AuthRoleRepository roleRepository;
    private final AuthRepository authRepository;
    private final AuthPositionRepository positionRepository;
    private final AuthDepartmentRepository departmentRepository;

    public void checkPlaceInfor(Long wardCode, Long provinceCode) {
        Place place = authRepository.getPlace(wardCode, provinceCode);
        if (place == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Place infor invalid");
        }
    }

    public List<AuthRole> checkRole(List<Role> roles) {
        List<String> codes = new ArrayList<>();
        for (Role item : roles) {
            codes.add(item.getCode());
        }
        List<AuthRole> authRoles = roleRepository.getRolesByCode(codes);
        if (authRoles == null || authRoles.isEmpty() || authRoles.size() != codes.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Role is invalid");
        }
        return authRoles;
    }

    public void checkDuplicatedDataUser(String mobile, String numberId, String personalEmail) {
        List<DuplicateUserrData> dupliactes = authRepository.checkDuplicateUserrData(mobile, numberId, personalEmail);
        if (dupliactes != null && !dupliactes.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "User existing ");
        }
    }

    public void checkDob(LocalDate dob) {
        if (dob == null) return;
        if (Period.between(dob, LocalDate.now()).getYears() < 18) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Người dùng phải đủ 18 tuổi");
        }
    }

    public int countValidUser(Long userId, String employeeCode) {
        return authRepository.countValidUser(userId, employeeCode);
    }

    public void checkPosition(String position) {
        if (position == null || position.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Position is invalid");
        }
        List<AuthPosition> positions = positionRepository.getPositionBayCode(position);
        if (positions == null || positions.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Position is invalid");
        }
    }

    public void checkDepartment(Long departmentId) {
        List<AuthDepartment> departments = departmentRepository.findActiveById(departmentId);
        if (departments == null || departments.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Deparment not existed");
        }
    }

    public AuthRole checkRoleById(Long roleId) {
        if (roleId == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND.getCode(), ErrorCode.ROLE_NOT_FOUND.getDesc());
        }
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND.getCode(), ErrorCode.ROLE_NOT_FOUND.getDesc()));
    }

}
