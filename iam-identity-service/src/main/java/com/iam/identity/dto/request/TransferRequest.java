package com.iam.identity.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TransferRequest {

    @NotNull
    private List<Long> roleIds;

    @NotBlank
    private String positionCode;

    @NotNull
    private Long departmentId;

    @NotNull
    private LocalDate transferDate;
}
