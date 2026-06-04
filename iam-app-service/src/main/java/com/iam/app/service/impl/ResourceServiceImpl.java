package com.iam.app.service.impl;

import com.iam.app.domain.AuthApplication;
import com.iam.app.domain.AuthDefaultResource;
import com.iam.app.domain.AuthResource;
import com.iam.app.dto.request.CreateResourceRequest;
import com.iam.app.dto.request.UpdateResourceRequest;
import com.iam.app.dto.response.AppWarning;
import com.iam.app.dto.response.BatchCreateResourceResponse;
import com.iam.app.dto.response.ResourceDetailResponse;
import com.iam.app.dto.response.ResourceListResponse;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.repository.jpa.AuthApplicationRepository;
import com.iam.app.repository.jpa.AuthDefaultResourceRepository;
import com.iam.app.repository.jpa.AuthResourceRepository;
import com.iam.app.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final AuthResourceRepository resourceRepository;
    private final AuthApplicationRepository applicationRepository;
    private final AuthDefaultResourceRepository defaultResourceRepository;

    @Override
    public Page<ResourceListResponse> getResources(Long appId, String status, String type,
            String name, String resourceCode, Pageable pageable) {
        String nameFilter = name != null ? "%" + name + "%" : null;
        return resourceRepository
                .findByFilters(appId, status, type, nameFilter, resourceCode,
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                .map(ResourceListResponse::new);
    }

    @Override
    public ResourceDetailResponse getResourceDetail(Long appId, Long resourceId) {
        AuthApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy ứng dụng"));
        AuthResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy tài nguyên"));
        if (!resource.getAppId().equals(appId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Tài nguyên không thuộc ứng dụng này");
        }
        return new ResourceDetailResponse(resource, app);
    }

    @Override
    @Transactional
    public BatchCreateResourceResponse createResources(Long appId, CreateResourceRequest request) {
        AuthApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy ứng dụng"));
        if (!"ACTIVE".equals(app.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Ứng dụng đã bị vô hiệu hóa, không thể thêm tài nguyên");
        }

        List<CreateResourceRequest.Item> items = request.getItems();

        // Check intra-batch duplicate resourceCode
        Set<String> seen = new HashSet<>();
        for (CreateResourceRequest.Item item : items) {
            if (!seen.add(item.getResourceCode())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Danh sách tạo có resourceCode trùng nhau: " + item.getResourceCode());
            }
        }

        // Check duplicate action tokens within each item
        for (CreateResourceRequest.Item item : items) {
            String[] tokens = item.getActions().split(",");
            Set<String> actionSet = new HashSet<>(Arrays.asList(tokens));
            if (actionSet.size() < tokens.length) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Actions của resource '" + item.getResourceCode() + "' có giá trị trùng nhau");
            }
        }

        // Find which resourceCodes already exist in DB
        Set<String> existingCodes = new HashSet<>(
                resourceRepository.findExistingResourceCodes(appId, new ArrayList<>(seen)));

        List<AuthResource> toSave = new ArrayList<>();
        List<BatchCreateResourceResponse.WarningItem> warnings = new ArrayList<>();

        for (CreateResourceRequest.Item item : items) {
            if (existingCodes.contains(item.getResourceCode())) {
                warnings.add(new BatchCreateResourceResponse.WarningItem(
                        item.getResourceCode(), "ResourceCode đã tồn tại trong ứng dụng này"));
            } else {
                toSave.add(AuthResource.builder()
                        .appId(appId)
                        .resourceCode(item.getResourceCode())
                        .resourceName(item.getResourceName())
                        .resourceType(item.getResourceType())
                        .actions(item.getActions())
                        .ldapGroupName(item.getLdapGroupName())
                        .description(item.getDescription())
                        .build());
            }
        }

        List<ResourceListResponse> saved = resourceRepository.saveAll(toSave)
                .stream()
                .map(ResourceListResponse::new)
                .collect(Collectors.toList());

        return new BatchCreateResourceResponse(saved, warnings);
    }

    @Override
    @Transactional
    public ResourceDetailResponse updateResource(Long appId, Long resourceId, UpdateResourceRequest request) {
        AuthApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy ứng dụng"));
        AuthResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy tài nguyên"));
        if (!resource.getAppId().equals(appId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Tài nguyên không thuộc ứng dụng này");
        }

        // Check resourceCode uniqueness only if changed
        if (!Objects.equals(resource.getResourceCode(), request.getResourceCode())) {
            List<String> existing = resourceRepository.findExistingResourceCodes(
                    appId, List.of(request.getResourceCode()));
            if (!existing.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "ResourceCode '" + request.getResourceCode() + "' đã tồn tại trong ứng dụng này");
            }
            resource.setResourceCode(request.getResourceCode());
        }

        List<String> actionList = request.getActions();
        if (new HashSet<>(actionList).size() < actionList.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Actions có giá trị trùng nhau");
        }

        if (!Objects.equals(resource.getResourceName(), request.getResourceName())) {
            resource.setResourceName(request.getResourceName());
        }
        if (!Objects.equals(resource.getResourceType(), request.getResourceType())) {
            resource.setResourceType(request.getResourceType());
        }
        String newActions = String.join(",", actionList);
        if (!Objects.equals(resource.getActions(), newActions)) {
            resource.setActions(newActions);
        }
        if (!Objects.equals(resource.getLdapGroupName(), request.getLdapGroupName())) {
            resource.setLdapGroupName(request.getLdapGroupName());
        }
        if (!Objects.equals(resource.getDescription(), request.getDescription())) {
            resource.setDescription(request.getDescription());
        }
        if (!Objects.equals(resource.getStatus(), request.getStatus())) {
            resource.setStatus(request.getStatus());
        }

        resourceRepository.save(resource);
        ResourceDetailResponse response = new ResourceDetailResponse(resource, app);

        if ("INACTIVE".equals(request.getStatus())) {
            List<AuthDefaultResource> usages = defaultResourceRepository.findByResourceId(resourceId);
            if (!usages.isEmpty()) {
                List<AppWarning> warningList = usages.stream()
                        .map(dr -> new AppWarning("DEFAULT_RESOURCE", dr.getId(),
                                dr.getPositionCode() != null ? dr.getPositionCode() : "ROLE:" + dr.getRoleId()))
                        .collect(Collectors.toList());
                response.setWarnings(warningList);
            }
        }

        return response;
    }
}
