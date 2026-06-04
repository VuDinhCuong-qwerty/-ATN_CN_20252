package com.iam.app.service;

import com.iam.app.dto.request.CreateResourceRequest;
import com.iam.app.dto.request.UpdateResourceRequest;
import com.iam.app.dto.response.BatchCreateResourceResponse;
import com.iam.app.dto.response.ResourceDetailResponse;
import com.iam.app.dto.response.ResourceListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResourceService {

    Page<ResourceListResponse> getResources(Long appId, String status, String type,
                                            String name, String resourceCode, Pageable pageable);

    ResourceDetailResponse getResourceDetail(Long appId, Long resourceId);

    BatchCreateResourceResponse createResources(Long appId, CreateResourceRequest request);

    ResourceDetailResponse updateResource(Long appId, Long resourceId, UpdateResourceRequest request);
}
