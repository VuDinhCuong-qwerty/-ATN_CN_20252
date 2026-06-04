package com.iam.auth.exception;

import com.iam.auth.enums.ErrorCode;
import lombok.Getter;


@Getter
public class AuthenticationException extends RuntimeException{
    private final String errorCode;
    private final String errorDesc;
    private final Object data;

    public AuthenticationException(String errorCode, String errorDesc, Object data) {
        super(errorDesc);
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
        this.data = data;
    }

    public AuthenticationException(ErrorCode error, Object data) {
        super(error.getDesc());
        this.errorCode = error.getCode();
        this.errorDesc = error.getDesc();
        this.data = data;
    }
}
