package com.iam.auth.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPasswordChangedPayload {
    private Long userId;
    private String employeeCode;
    private String username;
    private String email;
    private String eventType; // "CHANGE" | "RESET"
}
