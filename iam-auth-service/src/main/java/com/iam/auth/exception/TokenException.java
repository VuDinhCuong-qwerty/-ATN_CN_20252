package com.iam.auth.exception;

import com.iam.auth.enums.ErrorCode;
import lombok.Getter;

@Getter
public class TokenException extends RuntimeException {
    private final String error;
    private final String errorDesc;
    private final ErrorCode errorCode;

    public TokenException(ErrorCode code) {
        super(code.getDesc());
        this.error = code.getCode();
        this.errorDesc = code.getDesc();
        this.errorCode = code;
    }

    public TokenException(ErrorCode code, String desc) {
        super(desc);
        this.error = code.getCode();
        this.errorDesc = desc;
        this.errorCode = code;
    }

    public TokenException(ErrorCode errorCode, String code, String desc) {
        super(desc);
        this.error = code;
        this.errorDesc = desc;
        this.errorCode = errorCode;
    }
}
