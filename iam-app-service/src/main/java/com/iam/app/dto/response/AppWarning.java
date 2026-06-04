package com.iam.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppWarning {
    private String type; // CLIENT | RESOURCE
    private Long id;
    private String name;
}
