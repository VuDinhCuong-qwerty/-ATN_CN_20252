package com.iam.jobScheduled.connect.output;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
"name": "Thành phố Hà Nội",
    "code": 1,
    "division_type": "thành phố trung ương",
    "codename": "ha_noi",
    "phone_code": 24,
    "wards": []
*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Province {
    
    @JsonProperty("name")
    private String name;

    @JsonProperty("code")
    private Long code;

    @JsonProperty("division_type")
    private String divisionType;

    @JsonProperty("codename")
    private String codename;

    @JsonProperty("phone_code")
    private Integer phoneCode;

    @JsonProperty("wards")
    private List<Ward> wards;

}
