package com.iam.auth.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenIntrospectResponse {

    // RFC chuẩn
    @JsonProperty("active")
    private boolean active;

    @JsonProperty("sub")
    private String sub;

    @JsonProperty("client_id")
    private String clientId;      // serialize: "client_id"
    
    @JsonProperty("scope")
    private String scope;

    @JsonProperty("exp")
    private Long exp;

    @JsonProperty("iat")
    private Long iat;

    @JsonProperty("jti")
    private String jti;

    @JsonProperty("token_type")
    private String tokenType;     // serialize: "token_type"

    // User info
    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("display_name")
    private String displayName;   // serialize: "display_name"

    @JsonProperty("mobile")
    private String mobile;

    // Phân quyền
    @JsonProperty("role")
    private String role;

    @JsonProperty("permissions")
    private List<String> permissions;

    // App context

    @JsonProperty("app_id")
    private Long appId;           // serialize: "app_id"

    @JsonProperty("service_code")
    private String serviceCode;   // serialize: "service_code"
}
