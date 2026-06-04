package com.iam.app.service.impl;

import com.iam.app.domain.AuthClientGroup;
import com.iam.app.dto.request.CreateAppGroupRequest;
import com.iam.app.dto.request.UpdateAppGroupRequest;
import com.iam.app.dto.response.AppGroupResponse;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.repository.jpa.AuthApplicationRepository;
import com.iam.app.repository.jpa.AuthClientGroupRepository;
import com.iam.app.service.AppGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppGroupServiceImpl implements AppGroupService {

    private final AuthClientGroupRepository appGroupRepository;
    private final AuthApplicationRepository applicationRepository;

    @Override
    public List<AppGroupResponse> getAppGroups(Long id, Integer status) {
        return appGroupRepository.findWithFilters(id, status)
                .stream()
                .map(AppGroupResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public AppGroupResponse createAppGroup(CreateAppGroupRequest request) {
        if (appGroupRepository.countByName(request.getName()) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Tên nhóm ứng dụng đã tồn tại");
        }

        AuthClientGroup entity = AuthClientGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(1)
                .build();

        return new AppGroupResponse(appGroupRepository.save(entity));
    }

    @Override
    public AppGroupResponse updateAppGroup(Long id, UpdateAppGroupRequest request) {
        AuthClientGroup entity = appGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy nhóm ứng dụng"));

        // Guard: không thể xóa nếu còn ứng dụng thuộc nhóm
        if (request.getStatus() != null && request.getStatus() == 0
                && applicationRepository.countByGroupId(id) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Nhóm ứng dụng đang có ứng dụng, không thể xóa");
        }

        // Validate tên unique (loại trừ chính nó)
        if (appGroupRepository.countByNameAndIdNot(request.getName(), id) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Tên nhóm ứng dụng đã tồn tại");
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        return new AppGroupResponse(appGroupRepository.save(entity));
    }
}
