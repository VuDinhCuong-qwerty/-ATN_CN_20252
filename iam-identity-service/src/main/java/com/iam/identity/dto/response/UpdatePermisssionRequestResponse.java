package com.iam.identity.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermisssionRequestResponse {
    private String requestHeaderId;
    private String status;
    private LocalDateTime createAt;
    private LocalDateTime updatedAt;
}
