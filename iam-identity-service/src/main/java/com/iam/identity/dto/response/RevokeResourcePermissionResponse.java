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
public class RevokeResourcePermissionResponse {
    private List<Long> resourceIds;
    private int revokedCount;
    private LocalDateTime revokedAt;
}
