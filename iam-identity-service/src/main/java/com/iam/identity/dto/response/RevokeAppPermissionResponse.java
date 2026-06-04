package com.iam.identity.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevokeAppPermissionResponse {
    private List<Long> revokedResourceIds;
    private List<Long> revokedAppIds;
    private int revokedResourceCount;
    private int revokedAppCount;
    private LocalDateTime revokedAt;
}
