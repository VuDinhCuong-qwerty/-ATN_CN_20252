package com.demo.change.constant;

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

    // ── IDENTITY SERVICE (prefix: ID) ────────────────────
    USER_NOT_FOUND("ID01", "Không tìm thấy người dùng"),
    USER_ALREADY_EXISTS("ID02", "Người dùng đã tồn tại"),
    ROLE_NOT_FOUND("ID03", "Không tìm thấy role"),
    PERMISSION_NOT_FOUND("ID04", "Không tìm thấy permission"),
    PASSWORD_POLICY_VIOLATION("ID05", "Mật khẩu không đáp ứng chính sách"),
    PASSWORD_RECENTLY_USED("ID06", "Mật khẩu đã được sử dụng gần đây"),
    ROLE_ALREADY_EXISTS("ID07", "Role đã tồn tại"),
    PERMISSION_ALREADY_EXISTS("ID08", "Permission đã tồn tại"),
    ROLE_ALREADY_ASSIGNED("ID09", "Role đã được gán cho người dùng"),
    PERMISSION_ALREADY_GRANTED("ID10", "Permission đã được cấp cho role"),
    USER_DELETED("ID11", "Người dùng đã bị xóa khỏi hệ thống"),
    USER_ALREADY_OFFBOARDED("ID12", "Người dùng đã nghỉ việc khỏi hệ thống"),
    USER_ALREADY_ACTIVE("ID13", "Người dùng đang hoạt động trong hệ thống");

    private final String code;
    private final String desc;
}
