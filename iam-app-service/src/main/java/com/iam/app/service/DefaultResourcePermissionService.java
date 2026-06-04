package com.iam.app.service;

import com.iam.app.dto.request.BatchUpdateDefaultResourcePermissionRequest;
import com.iam.app.dto.request.CreateDefaultResourcePermissionRequest;
import com.iam.app.dto.response.DefaultResourcePermissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DefaultResourcePermissionService {

    Page<DefaultResourcePermissionResponse> getPermissions(Long roleId, String positionCode,
                                                           Long resourceId, String status,
                                                           Pageable pageable);

    List<DefaultResourcePermissionResponse> createPermissions(CreateDefaultResourcePermissionRequest request);

    List<DefaultResourcePermissionResponse> updatePermissions(BatchUpdateDefaultResourcePermissionRequest request);
}
