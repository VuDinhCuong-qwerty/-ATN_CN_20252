package com.iam.app.dto.pojo;

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
public class Department {
    private Long id;
    private String code;
    private String name;
    private Long parentId;
    private String detail;
}
