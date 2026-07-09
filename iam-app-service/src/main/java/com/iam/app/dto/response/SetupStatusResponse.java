package com.iam.app.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * "Trạng thái thiết lập" của 1 app — tính động (read-only, không lưu DB), chỉ để hiển thị
 * tiến trình cấu hình cho admin/wizard. Hoàn toàn tách biệt với AuthApplication.status
 * (ACTIVE/INACTIVE — bật/tắt truy cập, không liên quan tới mức độ hoàn thiện cấu hình).
 */
@Getter
@Builder
public class SetupStatusResponse {

    private Long appId;
    private String appType;
    private List<Step> steps;
    private String nextStep;
    private boolean overallComplete;

    @Getter
    @Builder
    public static class Step {
        private String key;
        private String label;
        private boolean done;
        /** false = tuỳ chọn, không chặn overallComplete (vd: Resource/DefaultPerm của app THIRD_PARTY_LDAP quản lý tài nguyên riêng, như GitLab). */
        private boolean required;
    }
}
