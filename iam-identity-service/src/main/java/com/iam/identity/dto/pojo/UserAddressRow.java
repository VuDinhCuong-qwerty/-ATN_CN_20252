package com.iam.identity.dto.pojo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserAddressRow {
    private String type;
    private Long   wardCode;
    private Long   provinceCode;
    private String wardName;
    private String provinceName;
    private String detail;
}
