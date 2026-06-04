package com.iam.identity.dto.pojo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDepartmentRow {
    private Long   departmentId;
    private String code;
    private String name;
    private Long   parentId;
    private int    depth;
}
