package com.iam.notify.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

// Matches UserPasswordChangedPayload from iam-identity-service
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordChangedPayload {
    private Long userId;
    private String employeeCode;
    private String username;
    private String email;
    private String eventType; // "CHANGE" | "RESET"
    private String password;  // non-null only when eventType = "RESET"
}
