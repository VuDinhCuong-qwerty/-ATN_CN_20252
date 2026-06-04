package com.iam.app.service;

import com.iam.app.dto.request.BatchUpdateDefaultAppPermissionStatusRequest;
import com.iam.app.dto.request.CreateDefaultAppPermissionRequest;
import com.iam.app.dto.response.DefaultAppPermissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DefaultAppPermissionService {

    Page<DefaultAppPermissionResponse> getPermissions(String roleId, String positionCode,
                                                      Long applicationId, String status,
                                                      Pageable pageable);

    List<DefaultAppPermissionResponse> createPermissions(CreateDefaultAppPermissionRequest request);

    List<DefaultAppPermissionResponse> updateStatuses(BatchUpdateDefaultAppPermissionStatusRequest request);
}
