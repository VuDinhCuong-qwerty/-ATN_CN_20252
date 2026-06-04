package com.iam.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iam.identity.enums.ErrorCode;

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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    private String path;
    private T data;

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

    public static <T> ApiResponse<T> error(int httpStatus, String errorCode, String errorDesc, String path) {
        return ApiResponse.<T>builder()
                .status(httpStatus)
                .errorCode(errorCode)
                .errorDesc(errorDesc)
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(null)
                .build();
    }

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
