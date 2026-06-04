package com.iam.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.iam.app.dto.request.CreateApplicationRequest;
import com.iam.app.dto.request.UpdateApplicationRequest;
import com.iam.app.dto.response.ApplicationResponse;
import com.iam.app.dto.response.GetApplicationsResponse;
import com.iam.app.dto.response.GetDetailAppResponse;

public interface ApplicationService {

    Page<GetApplicationsResponse> getApps(String type, String status, String serviceCode, String name, String groupName, Pageable pageable);

    GetDetailAppResponse getDetailApp(Long id);

    ApplicationResponse createApp(CreateApplicationRequest request);

    ApplicationResponse updateApp(Long id, UpdateApplicationRequest request);

    ApplicationResponse toggleAppStatus(Long id, String status);
}
