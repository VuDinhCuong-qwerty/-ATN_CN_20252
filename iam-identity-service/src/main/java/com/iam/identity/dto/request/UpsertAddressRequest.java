package com.iam.identity.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpsertAddressRequest {

    private Long wardCode;
    private Long provinceCode;
    private String detail; // chỉ nhận số, thường hoa, dấu cách và dấu , không trống

}
