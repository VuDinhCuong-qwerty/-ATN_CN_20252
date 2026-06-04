package com.iam.identity.kafka.event.payload;

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
public class UserCreatedNotificationPayload {
    private Long userId;
    private String fullName;
    private String username;
    private String email;
    private String tempPassword;
    private String changePasswordLink;
    private String joinDate;
}
