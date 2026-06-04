package com.iam.identity.service.common;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.iam.identity.dto.request.ChangePasswordRequest;
import com.iam.identity.dto.request.LeaveRequest;
import com.iam.identity.dto.request.OnboardRequest;
import com.iam.identity.dto.request.ResetPasswordRequest;
import com.iam.identity.dto.request.TransferRequest;
import com.iam.identity.dto.request.UpdatePersonalInfoRequest;
import com.iam.identity.dto.request.UpdateUserProfileRequest;
import com.iam.identity.dto.request.UpsertAddressRequest;
import com.iam.identity.enums.ErrorCode;
import com.iam.identity.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateInput {

    private static final Set<String> VALID_GENDERS = Set.of("MALE", "FEMALE", "OTHER");
    private static final String CCCD_PATTERN    = "\\d{12}";
    private static final String MOBILE_PATTERN  = "^(\\+84|0)\\d{9,10}$";
    private final CheckInfor checkInfor;

    public void validateUpdateUserProfile(UpdateUserProfileRequest request) {
        if (hasValue(request.getGender())) {
            if (!VALID_GENDERS.contains(request.getGender().get())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
        if (hasValue(request.getCccd())) {
            if (!request.getCccd().get().matches(CCCD_PATTERN)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
        if (hasValue(request.getMobile())) {
            if (!request.getMobile().get().matches(MOBILE_PATTERN)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
        if (hasValue(request.getDob())) {
            if (!request.getDob().get().isBefore(LocalDate.now())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
        if (hasValue(request.getPosition())) {
            checkInfor.checkPosition(request.getPosition().get());
        }
        if (hasValue(request.getDepartmentId())) {
            try {
                Long.parseLong(request.getDepartmentId().get());
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
    }

    public void validateUpsertAddress(String type, UpsertAddressRequest request) {
        if (type == null || !Set.of("PERMANENT", "TEMPORARY", "BIRTH").contains(type.toUpperCase())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getWardCode() == null || request.getProvinceCode() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    public void validateUpdatePersonalInfo(UpdatePersonalInfoRequest request) {
        // displayName không cho phép null khi present
        if (request.getDisplayName() != null) {
            if (!request.getDisplayName().isPresent() || request.getDisplayName().get().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
        if (hasValue(request.getEmailPersonal())) {
            if (!request.getEmailPersonal().get().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
        if (hasValue(request.getAddress())) {
            UpdatePersonalInfoRequest.AddressInfo addr = request.getAddress().get();
            if (addr.getType() == null
                    || !Set.of("PERMANENT", "TEMPORARY", "BIRTH").contains(addr.getType().toUpperCase())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
            if (addr.getWardCode() == null || addr.getProvinceCode() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }
    }

    public void validateLeaveRequest(LeaveRequest request) {
        if (request.getFromDate() == null || request.getToDate() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!request.getToDate().isAfter(request.getFromDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    public void validateOnboardRequest(OnboardRequest request) {
        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getPositionCode() == null || request.getPositionCode().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getDepartmentId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getJoinDate() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    public void validateTransferRequest(TransferRequest request) {
        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getPositionCode() == null || request.getPositionCode().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getDepartmentId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getTransferDate() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    public void validateResetPasswordRequest(ResetPasswordRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getEmployeeCode() == null || request.getEmployeeCode().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getNewPass() == null || request.getNewPass().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!request.getNewPass().equals(request.getConfirmNewPass())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        validatePasswordPolicy(request.getNewPass());
    }

    public void validateChangePasswordRequest(ChangePasswordRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getEmployeeCode() == null || request.getEmployeeCode().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getOldpass() == null || request.getOldpass().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (request.getNewPass() == null || request.getNewPass().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!request.getNewPass().equals(request.getConfirmNewPass())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        validatePasswordPolicy(request.getNewPass());
    }

    private void validatePasswordPolicy(String password) {
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION);
        }
    }

    // field != null → JSON field present; field.isPresent() → value not null
    private <T> boolean hasValue(Optional<T> field) {
        return field != null && field.isPresent();
    }

}
