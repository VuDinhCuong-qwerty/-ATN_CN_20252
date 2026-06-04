package com.iam.app.service;

import java.util.List;

import com.iam.app.dto.response.GetDepartmentResponse;
import com.iam.app.dto.response.GetPositionsResponse;
import com.iam.app.dto.response.GetRolesResponse;

public interface ReferenceService {

    List<GetRolesResponse> getRoles();

    List<GetPositionsResponse> getPositions(String status);

    List<GetDepartmentResponse> getDepartments(Long status);
    
}
