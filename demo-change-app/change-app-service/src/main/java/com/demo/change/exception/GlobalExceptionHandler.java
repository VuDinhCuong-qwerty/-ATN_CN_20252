package com.demo.change.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.demo.change.constant.ApiResponse;
import com.demo.change.constant.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── @Valid trên @RequestBody ──────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed [{}]: {}", request.getRequestURI(), detail);
        return ResponseEntity.badRequest().body(
                ApiResponse.error(ErrorCode.VALIDATION_FAILED, detail, request.getRequestURI()));
    }

    // ── @Validated trên method params ────────────────────────────────────────

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    int dot = path.lastIndexOf('.');
                    return (dot >= 0 ? path.substring(dot + 1) : path) + ": " + cv.getMessage();
                })
                .collect(Collectors.joining("; "));
        log.warn("ConstraintViolation [{}]: {}", request.getRequestURI(), detail);
        return ResponseEntity.badRequest().body(
                ApiResponse.error(ErrorCode.VALIDATION_FAILED, detail, request.getRequestURI()));
    }

    // ── @RequestParam bắt buộc bị thiếu ──────────────────────────────────────

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String detail = "Thiếu tham số bắt buộc: '" + ex.getParameterName() + "'";
        log.warn("Missing param [{}]: {}", request.getRequestURI(), detail);
        return ResponseEntity.badRequest().body(
                ApiResponse.error(ErrorCode.VALIDATION_FAILED, detail, request.getRequestURI()));
    }

    // ── @RequestParam sai kiểu dữ liệu ───────────────────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "không xác định";
        String detail = "Tham số '" + ex.getName() + "' phải là kiểu " + expected;
        log.warn("Type mismatch [{}]: {}", request.getRequestURI(), detail);
        return ResponseEntity.badRequest().body(
                ApiResponse.error(ErrorCode.VALIDATION_FAILED, detail, request.getRequestURI()));
    }

    // ── JSON không đọc được / thiếu trường bắt buộc trong body ───────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.error(ErrorCode.VALIDATION_FAILED,
                        "Request body không hợp lệ hoặc thiếu trường bắt buộc",
                        request.getRequestURI()));
    }

    // ── Spring Security: thiếu quyền (403) ───────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error(ErrorCode.FORBIDDEN, request.getRequestURI()));
    }

    // ── BusinessException: lỗi nghiệp vụ ────────────────────────────────────

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("BusinessException [{}]: {} - {}", request.getRequestURI(), ex.getErrorCode(), ex.getErrorDesc());
        return ResponseEntity.badRequest().body(
                ApiResponse.error(ex.getErrorCode(), ex.getErrorDesc(), request.getRequestURI()));
    }

    // ── Fallback: mọi exception chưa được xử lý ──────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(
                ApiResponse.unknown(request.getRequestURI()));
    }
}
