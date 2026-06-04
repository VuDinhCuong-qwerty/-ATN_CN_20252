package com.iam.identity.dto.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {
    private String wardCode;
    private String provinceCode;
    private String wardName;
    private String provinceName;
}
