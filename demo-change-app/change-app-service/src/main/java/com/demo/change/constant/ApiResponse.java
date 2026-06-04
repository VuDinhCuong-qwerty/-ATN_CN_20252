package com.demo.change.constant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String errorCode;
    private String errorDesc;
    private LocalDateTime timestamp;
    private String path;
    private T data;

    // Thành công
    public static <T> ApiResponse<T> ok(T data, String path) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .errorCode(ErrorCode.SUCCESS.getCode())
                .errorDesc(ErrorCode.SUCCESS.getDesc())
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(data)
                .build();
    }

    // Lỗi nghiệp vụ — dùng desc mặc định của ErrorCode
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String path) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode.getCode())
                .errorDesc(errorCode.getDesc())
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(null)
                .build();
    }

    // Lỗi nghiệp vụ — custom desc
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String errorDesc, String path) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode.getCode())
                .errorDesc(errorDesc)
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(null)
                .build();
    }

    // Lỗi nghiệp vụ — String errorCode (dùng cho BusinessException)
    public static <T> ApiResponse<T> error(String errorCode, String errorDesc, String path) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode)
                .errorDesc(errorDesc)
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(null)
                .build();
    }

    // Lỗi không xác định
    public static <T> ApiResponse<T> unknown(String path) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.UNKNOWN.getCode())
                .errorDesc(ErrorCode.UNKNOWN.getDesc())
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(null)
                .build();
    }
}
