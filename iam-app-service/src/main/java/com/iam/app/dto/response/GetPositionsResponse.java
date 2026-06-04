package com.iam.app.dto.response;

import com.iam.app.domain.AuthPosition;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetPositionsResponse {
    private String code;
    private String name;
    private String description;
    private String status; // ACTIVE/INACTIVE

    public GetPositionsResponse(AuthPosition position) {
        this.code = position.getCode();
        this.name = position.getName();
        this.description = position.getDescription();
        this.status = position.getStatus();
    }
}
