package com.iam.identity.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RevokeAppPermissionRequest {
    
    private List<Long> apps;
    @Size(max = 500)
    private String reason;
}
