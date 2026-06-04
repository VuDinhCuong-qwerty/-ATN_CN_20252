package com.iam.notify.service;

import com.iam.notify.kafka.payload.PasswordChangedPayload;
import com.iam.notify.kafka.payload.PermissionApprovedPayload;
import com.iam.notify.kafka.payload.PermissionRequestPayload;
import com.iam.notify.kafka.payload.UserCreatedPayload;

public interface EmailService {

    // ── User lifecycle ────────────────────────────────────────────────────────

    /**
     * Gửi email chào mừng + mật khẩu tạm thời khi tạo tài khoản mới.
     * Tự động tra EMAIL_PERSONAL từ AUTH_USER_PROFILE theo userId trong payload.
     * Fallback về CC_ADMIN nếu không tìm thấy email cá nhân.
     */
    void sendUserCreatedEmail(UserCreatedPayload payload);

    // ── Password ──────────────────────────────────────────────────────────────

    /** Cảnh báo bảo mật khi user TỰ đổi mật khẩu (eventType = CHANGE). */
    void sendPasswordChangedEmail(PasswordChangedPayload payload);

    /** Gửi mật khẩu mới khi admin RESET mật khẩu (eventType = RESET). */
    void sendPasswordResetEmail(PasswordChangedPayload payload);

    // ── Permission request ────────────────────────────────────────────────────

    /** Thông báo đến reviewer (CAB) khi có yêu cầu mới cần duyệt. */
    void sendPermissionRequestEmail(PermissionRequestPayload payload, String reviewerEmail);

    /** Xác nhận đến requester rằng yêu cầu đã được gửi thành công. */
    void sendPermissionSubmittedEmail(PermissionRequestPayload payload, String requesterEmail);

    /** Thông báo kết quả (APPROVED / REJECTED) đến requester. */
    void sendPermissionApprovedEmail(PermissionApprovedPayload payload, String requesterEmail);
}
