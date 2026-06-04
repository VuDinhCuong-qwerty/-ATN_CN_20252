package com.iam.app.service;

import com.iam.app.dto.request.CreateAppGroupRequest;
import com.iam.app.dto.request.UpdateAppGroupRequest;
import com.iam.app.dto.response.AppGroupResponse;

import java.util.List;

public interface AppGroupService {

    List<AppGroupResponse> getAppGroups(Long id, Integer status);

    AppGroupResponse createAppGroup(CreateAppGroupRequest request);

    AppGroupResponse updateAppGroup(Long id, UpdateAppGroupRequest request);
}
