package com.iam.identity.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
public class UserStatusResponse {

    private String employeeCode;
    private String status;
    private LocalDateTime inactiveFromDate;
    private LocalDateTime inactiveToDate;
    private LocalDateTime offboardedAt;
    private LocalDateTime onboardedAt;
    private LocalDateTime transferredAt;
    private String position;
    private Long departmentId;
    private LocalDate joinDate;
    private LocalDate transferDate;
}
