package com.iam.app.dto.response;

import com.iam.app.domain.AuthApplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetApplicationsResponse {
    private Long id;
    private String code;
    private String name;
    private String type;
    private String defaultUrl;
    private String logoUrl;
    private String status; // ACTIVE/INACTIVE

    public GetApplicationsResponse(AuthApplication application) {
        this.id = application.getId();
        this.code = application.getServiceCode();
        this.name = application.getName();
        this.type = application.getAppType();
        this.defaultUrl = application.getDefaultUrl();
        this.logoUrl = application.getLogoUri();
        this.status = application.getStatus();

    }
}
