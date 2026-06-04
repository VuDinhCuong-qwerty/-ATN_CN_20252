package com.iam.app.exception;

import com.iam.app.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String errorDesc;

    // Dùng desc mặc định của ErrorCode
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDesc());
        this.errorCode = errorCode;
        this.errorDesc = errorCode.getDesc();
    }

    // Dùng desc tùy chỉnh
    public BusinessException(ErrorCode errorCode, String errorDesc) {
        super(errorDesc);
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }
}
