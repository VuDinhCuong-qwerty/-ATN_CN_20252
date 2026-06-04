package com.iam.app.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class BatchCreateResourceResponse {

    private final List<ResourceListResponse> saved;
    private final List<WarningItem> warnings;

    public BatchCreateResourceResponse(List<ResourceListResponse> saved, List<WarningItem> warnings) {
        this.saved = saved;
        this.warnings = warnings;
    }

    @Getter
    public static class WarningItem {
        private final String resourceCode;
        private final String reason;

        public WarningItem(String resourceCode, String reason) {
            this.resourceCode = resourceCode;
            this.reason = reason;
        }
    }
}
