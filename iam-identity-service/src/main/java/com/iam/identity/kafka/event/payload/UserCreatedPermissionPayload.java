package com.iam.identity.kafka.event.payload;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedPermissionPayload {
    private Long userId;
    private List<String> roles;
    private String positionCode;
    private String departmentId;
}
