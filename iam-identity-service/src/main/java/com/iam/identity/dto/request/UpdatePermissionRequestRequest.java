package com.iam.identity.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionRequestRequest {

    @NotNull
    private Long requestId; // ID của AUTH_REQUEST_HEADER cần update

    private Long requesterId;

    @NotBlank
    private String requester; 

    @NotBlank
    private String requesterCode;

    private String requestForCode;

    @NotBlank
    private String reviewer; // uername
    @NotBlank
    private String reviewerCode; // mã nhân viên của CAB duyệt
    @NotBlank
    @Size(max = 500)
    private String reason; // lý do xin quyền

    @NotBlank
    @Pattern(regexp = "DRAFT|OFFICIAL")
    private String type;

    @Valid
    private List<App> apps;
    @Valid
    private List<Resource> resources;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class App {
        @NotNull
        private Long appId;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Resource {
        @NotNull
        private Long resourceId;
        @NotBlank
        private String actions;
    }

    public interface STATUS {
        String DRAFT    = "DRAFT";
        String OFFICIAL = "OFFICIAL";
    }

}