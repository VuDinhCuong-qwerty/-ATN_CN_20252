package com.iam.notify.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

// Matches UserCreatedNotificationPayload from iam-identity-service
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCreatedPayload {
    private Long userId;
    private String fullName;
    private String username;
    private String email;
    private String tempPassword;
    private String changePasswordLink;
    private String joinDate;
}
