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
public class UserPasswordChangedPayload {
    private Long userId;
    private String employeeCode;
    private String username;
    private String email;
    private String eventType; // CHANGE | RESET
    private String password;
}
