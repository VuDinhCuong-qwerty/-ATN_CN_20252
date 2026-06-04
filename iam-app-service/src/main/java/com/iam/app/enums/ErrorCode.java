package com.iam.app.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ── COMMON ──────────────────────────────────────────
    SUCCESS("00", "Thành công"),
    UNKNOWN("99", "Lỗi không xác định"),
    VALIDATION_FAILED("01", "Dữ liệu đầu vào không hợp lệ"),
    UNAUTHORIZED("02", "Không có quyền truy cập"),
    FORBIDDEN("03", "Bị từ chối truy cập"),
    NOT_FOUND("04", "Không tìm thấy tài nguyên"),

    // ── AUTH SERVICE (prefix: AU) ────────────────────────
    INVALID_CREDENTIALS("AU01", "Tên đăng nhập hoặc mật khẩu không đúng"),
    ACCOUNT_LOCKED("AU02", "Tài khoản đã bị khóa"),
    ACCOUNT_DISABLED("AU03", "Tài khoản đã bị vô hiệu hóa"),
    TOKEN_INVALID("AU04", "Token không hợp lệ"),
    TOKEN_EXPIRED("AU05", "Token đã hết hạn"),
    MFA_REQUIRED("AU06", "Yêu cầu xác thực MFA"),
    MFA_INVALID("AU07", "Mã MFA không đúng"),
    MFA_EXPIRED("AU08", "Mã MFA đã hết hạn"),
    SESSION_EXPIRED("AU09", "Phiên đăng nhập đã hết hạn"),

    // ── app SERVICE (prefix: ID) ────────────────────
    USER_NOT_FOUND("ID01", "Không tìm thấy người dùng"),
    USER_ALREADY_EXISTS("ID02", "Người dùng đã tồn tại"),
    ROLE_NOT_FOUND("ID03", "Không tìm thấy role"),
    PERMISSION_NOT_FOUND("ID04", "Không tìm thấy permission"),
    PASSWORD_POLICY_VIOLATION("ID05", "Mật khẩu không đáp ứng chính sách"),
    PASSWORD_RECENTLY_USED("ID06", "Mật khẩu đã được sử dụng gần đây"),

    // ── CLIENT SERVICE (prefix: CL) ──────────────────────
    CLIENT_NOT_FOUND("CL01", "Không tìm thấy client"),
    CLIENT_ALREADY_EXISTS("CL02", "Client đã tồn tại"),
    CLIENT_DISABLED("CL03", "Client đã bị vô hiệu hóa"),
    INVALID_CLIENT_SECRET("CL04", "Client secret không hợp lệ"),
    SCOPE_NOT_ALLOWED("CL05", "Scope không được phép");

    private final String code;
    private final String desc;
}
