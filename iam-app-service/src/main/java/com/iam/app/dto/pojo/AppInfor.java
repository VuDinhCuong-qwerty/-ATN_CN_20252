package com.iam.app.dto.pojo;

import java.time.LocalDateTime;

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
public class AppInfor {
    private Long id;
    private String name;
    private String description;
    private String appType;/** INTERNAL | THIRD_PARTY_LDAP */
    private String logoUri;
    private String defaultUrl;
    private String status;/** ACTIVE | INACTIVE */
    private Long departmentId;
    private String deparmentDetail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String serviceCode;
    private Long groupId;
    private String groupName;
    private Long acrLevel;
}
