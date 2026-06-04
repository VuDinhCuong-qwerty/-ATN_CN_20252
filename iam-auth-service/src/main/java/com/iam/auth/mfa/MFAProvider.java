package com.iam.auth.mfa;

/**
 * Interface cho MFA Provider — extensible theo plugin pattern.
 * Muốn thêm loại MFA mới chỉ cần implement interface này.
 *
 * Built-in implementations:
 * - OtpEmailMFAProvider  — gửi OTP qua email
 * - TotpMFAProvider      — Google Authenticator / TOTP
 */
public interface MFAProvider {

    /**
     * Gửi mã xác thực đến user
     * @param userId  ID của user
     * @param target  Email / phone number / device token
     */
    void send(String userId, String target);

    /**
     * Xác thực mã user nhập vào
     * @param userId  ID của user
     * @param code    Mã user nhập
     * @return true nếu hợp lệ
     */
    boolean verify(String userId, String code);

    /**
     * Loại MFA — dùng để map với config của client
     * Ví dụ: "OTP_EMAIL", "TOTP", "SMS"
     */
    String getType();
}
