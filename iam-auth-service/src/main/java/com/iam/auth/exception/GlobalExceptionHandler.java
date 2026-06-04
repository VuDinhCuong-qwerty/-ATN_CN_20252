package com.iam.auth.exception;

import com.iam.auth.config.AuthProperties;
import com.iam.auth.dto.response.ApiResponse;
import com.iam.auth.dto.response.LoginResponse;
import com.iam.auth.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuthProperties authProperties;

    private static final String ERROR_PAGE_PATH = "/ms-internal-iam/auth/internal/error";

    private String errorPageRedirect(String errorCode) {
        String code = switch (errorCode != null ? errorCode : "") {
            case "03" -> "403";
            case "04" -> "404";
            default -> "400";
        };
        return "redirect:" + ERROR_PAGE_PATH + "?code=" + code;
    }

    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthentication(AuthenticationException e,
            HttpServletRequest request, HttpServletResponse response,
            HttpSession session) {
        log.error("Authentication error: {}", e.getMessage(), e);

        // Guard: data is not a LoginResponse (e.g. client not found, lock race) ->
        // error page
        if (!(e.getData() instanceof LoginResponse loginResponse)) {
            return errorPageRedirect(e.getErrorCode());
        }

        // TERMINAL_FAIL + redirectUri: redirect there directly (e.g. session expired ->
        // step 1)
        if (LoginResponse.STATUS.TERMINAL_FAIL.equals(loginResponse.getStatus())
                && loginResponse.getRedirectUri() != null) {
            session.removeAttribute("auth:ui:context");
            return "redirect:" + loginResponse.getRedirectUri();
        }

        String method = loginResponse.getMethod();

        // TERMINAL_FAIL without method: no login page context -> error page
        if (LoginResponse.STATUS.TERMINAL_FAIL.equals(loginResponse.getStatus()) && method == null) {
            session.removeAttribute("auth:ui:context");
            return errorPageRedirect(e.getErrorCode());
        }

        // FAIL (retryable) or TERMINAL_FAIL with method: update context, redirect to
        // login page
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) session.getAttribute("auth:ui:context");
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("authSessionId", loginResponse.getSessionId());
        data.put("clientId", loginResponse.getClientId());
        data.put("type", method);
        data.put("challengeInfo", loginResponse.getChallengeInfo());
        data.put("availableMethods", loginResponse.getAvailableMethods());
        data.put("errorCode", e.getErrorCode());
        data.put("errorDesc", e.getErrorDesc());
        data.remove("sessionToken");
        data.remove("tokenTimestamp");
        session.setAttribute("auth:ui:context", data);

        FlashMapManager flashMapManager = RequestContextUtils.getFlashMapManager(request);
        if (flashMapManager != null) {
            FlashMap flashMap = new FlashMap();
            flashMap.put("errorCode", e.getErrorCode());
            flashMap.put("errorDesc", e.getErrorDesc());
            flashMapManager.saveOutputFlashMap(flashMap, request, response);
        }

        String theme = loginResponse.getTheme() != null ? loginResponse.getTheme() : "default";
        return "redirect:" + authProperties.getEndpoints().getLoginPage()
                + "?client-id=" + loginResponse.getClientId()
                + "&theme=" + theme
                + "&action-type=" + method.toLowerCase();
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        log.error("Token error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorDesc(), request.getRequestURI()));
    }

    @ExceptionHandler(TokenException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleTokenResponse(TokenException e) {
        log.error("Token error occurred: {}", e.getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        int status = switch (errorCode) {
            case ErrorCode.UNKNOWN,
                    ErrorCode.SIGNING_KEY_NOT_FOUND,
                    ErrorCode.SIGNING_KEY_GENERATION_FAILED ->
                500;
            case ErrorCode.UNAUTHORIZED,
                    ErrorCode.INVALID_CLIENT ->
                401;
            case ErrorCode.FORBIDDEN,
                    ErrorCode.UNAUTHORIZED_CLIENT ->
                403;
            default -> 400;
        };

        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", e.getError(),
                        "error_description", e.getErrorDesc()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Token error occurred: {}", ex.getMessage(), ex);
        String desc = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, desc, request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseBody
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> handleUnknown(
            Exception ex, HttpServletRequest request) {
        log.error("Token error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.unknown(request.getRequestURI()));
    }
}
