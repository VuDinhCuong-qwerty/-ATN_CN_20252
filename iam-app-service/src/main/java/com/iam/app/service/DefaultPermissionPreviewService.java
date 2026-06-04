package com.iam.app.service;

import com.iam.app.dto.response.DefaultPermissionPreviewResponse;

public interface DefaultPermissionPreviewService {

    DefaultPermissionPreviewResponse preview(String roleCode, String positionCode);
}
