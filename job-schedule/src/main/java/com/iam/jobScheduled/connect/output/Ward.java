package com.iam.jobScheduled.connect.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
{
    "name": "Phường Ba Đình",
    "code": 4,
    "division_type": "phường",
    "codename": "phuong_ba_dinh",
    "province_code": 1
}
*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ward {
    @JsonProperty("name")
    private String name;
    @JsonProperty("code")
    private Long code;
    @JsonProperty("division_type")
    private String divisionType;
    @JsonProperty("codename")
    private String codename;
    @JsonProperty("province_code")
    private Long provinceCode;
}
