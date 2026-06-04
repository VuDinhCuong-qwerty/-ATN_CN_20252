package com.iam.identity.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iam.identity.domain.AuthRole;
import com.iam.identity.domain.AuthUser;
import com.iam.identity.domain.AuthUserAddress;
import com.iam.identity.domain.AuthUserProfile;
import com.iam.identity.domain.AuthUserRole;
import com.iam.identity.dto.request.CreateUserRequest;
import com.iam.identity.dto.request.UpdatePersonalInfoRequest;
import com.iam.identity.dto.request.UpdateUserProfileRequest;
import com.iam.identity.dto.request.UpsertAddressRequest;
import com.iam.identity.dto.response.AddressResponse;
import com.iam.identity.dto.response.CreateUserResponse;
import com.iam.identity.dto.response.UpdateUserResponse;
import com.iam.identity.dto.pojo.UserAddressRow;
import com.iam.identity.dto.pojo.UserDepartmentRow;
import com.iam.identity.dto.pojo.UserInfoRow;
import com.iam.identity.dto.pojo.UserRoleRow;
import com.iam.identity.dto.response.UserDetailResponse;
import com.iam.identity.dto.response.UserSummaryResponse;
import com.iam.identity.config.KafkaConfig;
import com.iam.identity.config.context.RequestContext;
import com.iam.identity.enums.ErrorCode;
import com.iam.identity.exception.BusinessException;
import com.iam.identity.kafka.event.payload.UserCreatedNotificationPayload;
import com.iam.identity.kafka.event.payload.UserCreatedPermissionPayload;
import com.iam.identity.kafka.producer.IdentityEventProducer;
import com.iam.identity.repository.cache.DataCached;
import com.iam.identity.repository.jpa.AuthRepository;
import com.iam.identity.domain.AuthProvince;
import com.iam.identity.domain.AuthWard;
import com.iam.identity.dto.response.ProvinceResponse;
import com.iam.identity.dto.response.WardResponse;
import com.iam.identity.repository.jpa.AuthProvinceRepository;
import com.iam.identity.repository.jpa.AuthUserAddressRepository;
import com.iam.identity.repository.jpa.AuthUserProfileRepository;
import com.iam.identity.repository.jpa.AuthUserRepository;
import com.iam.identity.repository.jpa.AuthWardRepository;
import com.iam.identity.service.UserInfoService;
import com.iam.identity.service.common.CheckInfor;
import com.iam.identity.service.common.GenDataService;
import com.iam.identity.service.common.SavedService;
import com.iam.identity.service.common.ValidateInput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserInfoServiceImpl implements UserInfoService {

    private final CheckInfor checkInfor;
    private final GenDataService genDataService;
    private final SavedService savedService;
    private final ValidateInput validateService;
    private final PasswordEncoder passwordEncoder;
    private final DataCached dataCached;
    private final IdentityEventProducer identityEventProducer;
    private final AuthRepository authRepository;
    private final AuthUserProfileRepository authUserProfileRepository;
    private final AuthUserRepository authUserRepository;
    private final AuthUserAddressRepository authUserAddressRepository;
    private final AuthProvinceRepository provinceRepository;
    private final AuthWardRepository wardRepository;

    private final String POSTFIX_EMAIL = "@bank.com.vn";

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        checkInfor.checkDuplicatedDataUser(request.getMobile(), request.getNumberId(), request.getMail());
        checkInfor.checkPlaceInfor(request.getTemporaryAddress().getWardCode(),
                request.getTemporaryAddress().getProvinceCode());
        checkInfor.checkPlaceInfor(request.getPermanentAddress().getWardCode(),
                request.getPermanentAddress().getProvinceCode());
        checkInfor.checkPlaceInfor(request.getBirthAddress().getWardCode(),
                request.getBirthAddress().getProvinceCode());
        checkInfor.checkPosition(request.getPosition());
        List<AuthRole> authRoles = checkInfor.checkRole(request.getRoles());

        String username = genDataService.genUsername(request.getFullName());
        log.info("username = {}", username);
        String password = genDataService.genPassword(10);
        AuthUser user = AuthUser.builder()
                .username(username)
                .email(username + POSTFIX_EMAIL)
                .mobile(request.getMobile())
                .displayName(username)
                .password(passwordEncoder.encode(password))
                .forceChangePassword(1)
                .build();

        String[] splitedNames = genDataService.splitFullName(request.getFullName());
        String employeeCode = genDataService.getEmployeeCode();
        AuthUserProfile userProfile = AuthUserProfile.builder()
                .firstName(splitedNames[0].toUpperCase())
                .lastName(splitedNames[1].toUpperCase())
                .fullName(request.getFullName().toUpperCase())
                .displayName(username.toUpperCase())
                .gender(request.getGender())
                .dob(request.getDob())
                .emailPersonal(request.getMail())
                .nationality(request.getNationality())
                .ethnic(request.getEthnic())
                .religion(request.getReligion())
                .avatarUrl(request.getAvatarUrl())
                .cccd(request.getNumberId())
                .cccdIssuedPlace(request.getNumberIdIssuedPlace())
                .cccdIssuedDate(request.getNumberIdIssuedDate())
                .joinDate(request.getJoinDate())
                .employeeCode(employeeCode)
                .departmentId(request.getDepartmentId())
                .position(request.getPosition())
                .build();
        List<AuthUserRole> authUserRoles = new ArrayList<>();
        for (AuthRole item : authRoles) {
            AuthUserRole authUserRole = AuthUserRole.builder()
                    .roleId(item.getId())
                    .grantedBy(RequestContext.getEmployeeCode())
                    .expiredAt(null)
                    .build();
            authUserRoles.add(authUserRole);
        }
        List<AuthUserAddress> userAddresses = new ArrayList<>();
        userAddresses.add(
                AuthUserAddress.builder()
                        .type(AuthUserAddress.Type.PERMANENT)
                        .provinceCode(request.getPermanentAddress().getProvinceCode())
                        .wardCode(request.getPermanentAddress().getWardCode())
                        .detail(request.getPermanentAddress().getDetail())
                        .build());
        userAddresses.add(
                AuthUserAddress.builder()
                        .type(AuthUserAddress.Type.TEMPORARY)
                        .provinceCode(request.getTemporaryAddress().getProvinceCode())
                        .wardCode(request.getTemporaryAddress().getWardCode())
                        .detail(request.getTemporaryAddress().getDetail())
                        .build());
        userAddresses.add(
                AuthUserAddress.builder()
                        .type(AuthUserAddress.Type.BIRTH)
                        .provinceCode(request.getBirthAddress().getProvinceCode())
                        .wardCode(request.getBirthAddress().getWardCode())
                        .detail(request.getBirthAddress().getDetail())
                        .build());

        CreateUserResponse output = savedService.createUser(user, userProfile, userAddresses,
                authUserRoles, request);

        List<String> permRoles = request.getRoles().stream()
                .map(r -> r.getCode())
                .collect(Collectors.toList());

        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_DEFAULT_GRANT_PERMISSION_USER,
                    "DEFAULT-GRANT-PERMISSION-USER",
                    UserCreatedPermissionPayload.builder()
                            .userId(output.getUserId())
                            .roles(permRoles)
                            .positionCode(request.getPosition())
                            .departmentId(String.valueOf(request.getDepartmentId()))
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish DEFAULT-GRANT-PERMISSION-USER for userId={}: {}",
                    output.getUserId(), e.getMessage());
        }

        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_CREATE_SUCCESS_USER_NOTIFY,
                    "CREATE-SUCCESS-USER-NOTIFY",
                    UserCreatedNotificationPayload.builder()
                            .userId(output.getUserId())
                            .fullName(output.getFullName())
                            .username(output.getUsername())
                            .email(output.getEmail())
                            .tempPassword(password)
                            .joinDate(request.getJoinDate().toString())
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish CREATE-SUCCESS-USER-NOTIFY for userId={}: {}", output.getUserId(),
                    e.getMessage());
        }

        return output;

    }

    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INACTIVE", "DELETED");
    private static final int MAX_PAGE_SIZE     = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    @Transactional
    public UserSummaryResponse getUsers(String username, String employeeCode,
            Long departmentId, String status, Boolean onLeave, Boolean offboarded, Pageable pageable) {

        // 1. Validate input
        boolean specialFilter = Boolean.TRUE.equals(onLeave) || Boolean.TRUE.equals(offboarded);
        if (!specialFilter && status != null && !VALID_STATUSES.contains(status.toUpperCase())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "status chỉ chấp nhận: ACTIVE | INACTIVE | DELETED");
        }
        if (departmentId != null && departmentId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "departmentId phải là số nguyên dương");
        }

        // 2. Sanitize pagination
        int page = Math.max(0, pageable.getPageNumber());
        int size = pageable.getPageSize() > 0
                ? Math.min(pageable.getPageSize(), MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;
        long offset = (long) page * size;
        // when onLeave/offboarded=true, status filter is handled by repository
        String normalizedStatus = specialFilter ? null : (status != null ? status.toUpperCase() : null);

        // 3. Query DB
        String normalizedUsername = username != null ? username.toUpperCase() : null;
        long total = authRepository.countUsers(normalizedUsername, employeeCode, departmentId, normalizedStatus, onLeave, offboarded);
        List<Object[]> rows = authRepository.getUsersList(
                normalizedUsername, employeeCode, departmentId, normalizedStatus, offset, size, onLeave, offboarded);

        // 4. Map rows → Content (gọi DataCached cho departmentDetail)
        List<UserSummaryResponse.Content> content = rows.stream()
                .map(r -> {
                    Long deptId = r[9] != null ? ((Number) r[9]).longValue() : null;
                    return UserSummaryResponse.Content.builder()
                            .userId(r[0] != null ? ((Number) r[0]).longValue() : null)
                            .username((String) r[1])
                            .email((String) r[2])
                            .mobile((String) r[3])
                            .status((String) r[4])
                            .employeeCode((String) r[5])
                            .fullName((String) r[6])
                            .position((String) r[7])
                            .joinDate(toLocalDate(r[8]))
                            .departmentId(deptId)
                            .departmentDetail(dataCached.getDetailDepartment(deptId))
                            .build();
                })
                .collect(Collectors.toList());

        // 5. Build response wrapper
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new UserSummaryResponse(content, total, totalPages, page, size);
    }

    private LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof java.sql.Date d) return d.toLocalDate();
        if (val instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        return null;
    }

    @Override
    public UserDetailResponse getUserDetail(Long userId, String employeeCode) {
        if (checkInfor.countValidUser(userId, employeeCode) != 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserInfoRow info = authRepository.getUserInfo(userId, employeeCode);
        if (info == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        List<UserAddressRow>    addressRows    = authRepository.getAddressesByUserId(userId);
        List<UserDepartmentRow> departmentRows = authRepository.getDepartmentsByUserId(userId);
        List<UserRoleRow>       roleRows       = authRepository.getRolesByUserId(userId);

        return UserDetailResponse.builder()
                .userId(info.getUserId())
                .employeeCode(info.getEmployeeCode())
                .username(info.getUsername())
                .email(info.getEmail())
                .emailPersonal(info.getEmailPersonal())
                .mobile(info.getMobile())
                .positionCode(info.getPositionCode())
                .position(info.getPosition())
                .status(info.getStatus())
                .firstName(info.getFirstName())
                .lastName(info.getLastName())
                .fullName(info.getFullName())
                .displayName(info.getDisplayName())
                .gender(info.getGender())
                .dob(info.getDob())
                .nationality(info.getNationality())
                .ethnic(info.getEthnic())
                .religion(info.getReligion())
                .avatarUrl(info.getAvatarUrl())
                .numberId(info.getNumberId())
                .numberIdIssuedDate(info.getNumberIdIssuedDate())
                .numberIdIssuedPlace(info.getNumberIdIssuedPlace())
                .joinDate(info.getJoinDate())
                .addresses(addressRows == null ? List.of() : addressRows.stream()
                        .map(a -> UserDetailResponse.AddressInfo.builder()
                                .type(a.getType())
                                .wardCode(a.getWardCode())
                                .provinceCode(a.getProvinceCode())
                                .wardName(a.getWardName())
                                .provinceName(a.getProvinceName())
                                .detail(a.getDetail())
                                .build())
                        .collect(Collectors.toList()))
                .departments(departmentRows == null ? List.of() : departmentRows.stream()
                        .map(d -> UserDetailResponse.DepartmentInfo.builder()
                                .departmentId(d.getDepartmentId())
                                .code(d.getCode())
                                .name(d.getName())
                                .parentId(d.getParentId())
                                .depth(d.getDepth())
                                .build())
                        .collect(Collectors.toList()))
                .roles(roleRows == null ? List.of() : roleRows.stream()
                        .map(r -> UserDetailResponse.RoleInfo.builder()
                                .roleCode(r.getRoleCode())
                                .roleName(r.getRoleName())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public UpdateUserResponse updateUserProfile(String employeeCode, UpdateUserProfileRequest request) {
        validateService.validateUpdateUserProfile(request);
        if (request.getDob() != null && request.getDob().isPresent()) {
            checkInfor.checkDob(request.getDob().get());
        }

        List<AuthUserProfile> profiles = authUserProfileRepository.findByEmployeeCode(employeeCode);
        if (profiles.isEmpty())
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        AuthUserProfile profile = profiles.get(0);

        Long userId = profile.getUserId();
        if (userId == null)
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users.isEmpty())
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        AuthUser user = users.get(0);

        if ("DELETED".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        applyOptional(request.getFirstName(), profile::setFirstName);
        applyOptional(request.getLastName(), profile::setLastName);
        applyOptional(request.getFullName(), profile::setFullName);
        applyOptional(request.getDob(), profile::setDob);
        applyOptional(request.getGender(), profile::setGender);
        applyOptional(request.getNationality(), profile::setNationality);
        applyOptional(request.getEthnic(), profile::setEthnic);
        applyOptional(request.getReligion(), profile::setReligion);
        applyOptional(request.getCccd(), profile::setCccd);
        applyOptional(request.getCccdIssuedDate(), profile::setCccdIssuedDate);
        applyOptional(request.getCccdIssuedPlace(), profile::setCccdIssuedPlace);
        applyOptional(request.getJoinDate(), profile::setJoinDate);
        applyOptional(request.getPosition(), profile::setPosition);

        if (request.getDepartmentId() != null) {
            profile.setDepartmentId(
                    request.getDepartmentId().isPresent()
                            ? Long.parseLong(request.getDepartmentId().get())
                            : null);
        }

        applyOptional(request.getMobile(), user::setMobile);

        LocalDateTime now = LocalDateTime.now();
        authUserProfileRepository.save(profile);
        authUserRepository.save(user);

        return UpdateUserResponse.builder()
                .userId(user.getId())
                .employeeCode(employeeCode)
                .updatedAt(now)
                .build();
    }

    private <T> void applyOptional(Optional<T> field, Consumer<T> setter) {
        if (field != null) {
            setter.accept(field.orElse(null));
        }
    }

    @Override
    @SuppressWarnings("null")
    @Transactional
    public UpdateUserResponse updatePersonalInfo(String employeeCode, UpdatePersonalInfoRequest request) {
        validateService.validateUpdatePersonalInfo(request);

        List<AuthUserProfile> profiles = authUserProfileRepository.findByEmployeeCode(employeeCode);
        if (profiles.isEmpty())
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        AuthUserProfile profile = profiles.get(0);

        Long userId = profile.getUserId();
        if (userId == null)
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users.isEmpty())
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if ("DELETED".equalsIgnoreCase(users.get(0).getStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        applyOptional(request.getDisplayName(), profile::setDisplayName);
        applyOptional(request.getAvatarUrl(), profile::setAvatarUrl);
        applyOptional(request.getEmailPersonal(), profile::setEmailPersonal);

        authUserProfileRepository.save(profile);

        if (request.getAddress() != null && request.getAddress().isPresent()) {
            UpdatePersonalInfoRequest.AddressInfo addr = request.getAddress().get();
            checkInfor.checkPlaceInfor(addr.getWardCode(), addr.getProvinceCode());
            String addressType = addr.getType().toUpperCase();
            List<AuthUserAddress> existing = authUserAddressRepository.findByUserIdAndType(userId, addressType);
            if (existing.isEmpty()) {
                authUserAddressRepository.save(AuthUserAddress.builder()
                        .userId(userId)
                        .type(addressType)
                        .provinceCode(addr.getProvinceCode())
                        .wardCode(addr.getWardCode())
                        .detail(addr.getDetail())
                        .build());
            } else {
                AuthUserAddress addrEntity = existing.get(0);
                addrEntity.setProvinceCode(addr.getProvinceCode());
                addrEntity.setWardCode(addr.getWardCode());
                addrEntity.setDetail(addr.getDetail());
                authUserAddressRepository.save(addrEntity);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        return UpdateUserResponse.builder()
                .userId(userId)
                .employeeCode(employeeCode)
                .updatedAt(now)
                .build();
    }

    @Override
    public List<AddressResponse> getAddresses(String employeeCode) {
        // 1. Tìm profile theo employeeCode
        List<AuthUserProfile> profiles = authUserProfileRepository.findByEmployeeCode(employeeCode);
        if (profiles.isEmpty())
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        AuthUserProfile profile = profiles.get(0);

        Long userId = profile.getUserId();
        if (userId == null)
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        // 2. Kiểm tra user hợp lệ (không DELETED)
        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users.isEmpty() || "DELETED".equalsIgnoreCase(users.get(0).getStatus()))
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        // 3. Lấy địa chỉ kèm tên tỉnh/phường
        List<Object[]> rows = authUserAddressRepository.findByUserIdWithNames(userId);

        // 4. Map sang AddressResponse
        // Thứ tự cột: type[0], province_code[1], province_name[2],
        //             ward_code[3], ward_name[4], detail[5], updated_at[6]
        return rows.stream()
                .map(r -> AddressResponse.builder()
                        .userId(userId)
                        .employeeCode(employeeCode)
                        .type((String) r[0])
                        .provinceCode(r[1] != null ? ((Number) r[1]).longValue() : null)
                        .provinceName((String) r[2])
                        .wardCode(r[3] != null ? ((Number) r[3]).longValue() : null)
                        .wardName((String) r[4])
                        .detail((String) r[5])
                        .updatedAt(r[6] instanceof java.sql.Timestamp ts
                                ? ts.toLocalDateTime() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("null")
    @Transactional
    public AddressResponse upsertAddress(String employeeCode, String type, UpsertAddressRequest request) {
        validateService.validateUpsertAddress(type, request);
        checkInfor.checkPlaceInfor(request.getWardCode(), request.getProvinceCode());

        List<AuthUserProfile> profiles = authUserProfileRepository.findByEmployeeCode(employeeCode);
        if (profiles.isEmpty())
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        AuthUserProfile profile = profiles.get(0);

        Long userId = profile.getUserId();
        if (userId == null)
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users.isEmpty())
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if ("DELETED".equalsIgnoreCase(users.get(0).getStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String normalizedType = type.toUpperCase();
        List<AuthUserAddress> existing = authUserAddressRepository.findByUserIdAndType(userId, normalizedType);
        if (existing.isEmpty()) {
            AuthUserAddress address = AuthUserAddress.builder()
                    .userId(userId)
                    .type(normalizedType)
                    .provinceCode(request.getProvinceCode())
                    .wardCode(request.getWardCode())
                    .detail(request.getDetail())
                    .build();
            authUserAddressRepository.save(address);
        } else {
            AuthUserAddress address = existing.get(0);
            address.setProvinceCode(request.getProvinceCode());
            address.setWardCode(request.getWardCode());
            address.setDetail(request.getDetail());
            authUserAddressRepository.save(address);
        }

        LocalDateTime now = LocalDateTime.now();

        return AddressResponse.builder()
                .userId(userId)
                .employeeCode(employeeCode)
                .type(type.toUpperCase())
                .provinceCode(request.getProvinceCode())
                .wardCode(request.getWardCode())
                .detail(request.getDetail())
                .updatedAt(now)
                .build();
    }

    @Override
    public List<ProvinceResponse> getProvinces(String name) {
        List<AuthProvince> list = (name != null && !name.isBlank())
                ? provinceRepository.findByStatusAndNameContaining(AuthProvince.Status.ACTIVE, name)
                : provinceRepository.findByStatus(AuthProvince.Status.ACTIVE);
        return list.stream()
                .map(p -> ProvinceResponse.builder()
                        .code(p.getCode())
                        .name(p.getName())
                        .divisionType(p.getDivisionType())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<WardResponse> getWards(Long provinceCode, String name) {
        List<AuthWard> list = (name != null && !name.isBlank())
                ? wardRepository.findByProvinceCodeAndStatusAndNameContaining(
                        provinceCode, AuthWard.Status.ACTIVE, name)
                : wardRepository.findByProvinceCodeAndStatus(provinceCode, AuthWard.Status.ACTIVE);
        return list.stream()
                .map(w -> WardResponse.builder()
                        .code(w.getCode())
                        .name(w.getName())
                        .divisionType(w.getDivisionType())
                        .provinceCode(w.getProvinceCode())
                        .build())
                .collect(Collectors.toList());
    }

}
