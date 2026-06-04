package com.iam.identity.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private List<Content> content;
    private long         totalElement;
    private int          totalPage;
    private int          currentPage;
    private int          pageSize;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private Long      userId;
        private String    employeeCode;
        private String    email;
        private String    mobile;
        private String    username;
        private String    fullName;
        private String    position;
        private Long      departmentId;
        private String    departmentDetail;
        private String    status;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate joinDate;
    }
}
