package com.demo.change.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDocumentRequest {
    @NotBlank
    private String docType;  // GIT/JIRA/CONFLUENCE/BUILD/OTHER
    @NotBlank
    private String title;
    private String url;
    private String note;
}
