package com.iam.identity.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RevokeResourcePermissionRequest {

    private List<Long> resourceIds;
    
    @Size(max = 200)
    private String reason;
}
