package com.iam.auth.enums;

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
    REFRESH_TOKEN_INVALID("AU10", "Refresh token không hợp lệ"),
    REFRESH_TOKEN_EXPIRED("AU11", "Refresh token đã hết hạn"),

    // ── SIGNING KEY ──────────────────────────────────────
    // ── CLIENT ───────────────────────────────────────────
    CLIENT_NOT_FOUND("CL01", "Không tìm thấy client"),

    // ── OAUTH2 TOKEN (RFC 6749 §5.2) ────────────────────
    INVALID_REQUEST       ("invalid_request",        "Yêu cầu không hợp lệ hoặc thiếu tham số"),
    INVALID_CLIENT        ("invalid_client",         "Xác thực client thất bại"),
    INVALID_GRANT         ("invalid_grant",          "Authorization grant không hợp lệ hoặc hết hạn"),
    UNAUTHORIZED_CLIENT   ("unauthorized_client",    "Client không được phép dùng grant type này"),
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type", "Grant type không được hỗ trợ"),
    INVALID_SCOPE         ("invalid_scope",          "Scope không hợp lệ hoặc vượt quá quyền"),


    // ── SIGNING KEY ──────────────────────────────────────
    SIGNING_KEY_NOT_FOUND("SK01", "Không tìm thấy khóa ký hợp lệ"),
    SIGNING_KEY_GENERATION_FAILED("SK02", "Không thể tạo cặp khóa ký"),

    // ──────── ERROR_PAGE ───────────────
    BAD_REQUEST_PAGE("BAD_REQUEST", "/template/default/error/img/400.png");
//    INTERNAL_SERVER_ERROR_PAGE("BAD_REQUEST", "/template/default/error/img/400.png");


    private final String code;
    private final String desc;
}
