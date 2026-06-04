package com.iam.app.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iam.app.domain.AuthDepartment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetDepartmentResponse {
    private Long id;
    private String code;
    private String name;
    @JsonIgnore
    private Long parentId; // trường này sẽ không đc trả ra ouput
    private List<GetDepartmentResponse> children;

    public GetDepartmentResponse(AuthDepartment department) {
        this.id = department.getId();
        this.code = department.getCode();
        this.name = department.getName();
        this.parentId = department.getParentId();
        this.children = new ArrayList<>();
    }
}
