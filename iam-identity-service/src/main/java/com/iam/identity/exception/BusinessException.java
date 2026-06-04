package com.iam.identity.exception;

import com.iam.identity.enums.ErrorCode;

import lombok.Getter;


@Getter
public class BusinessException extends RuntimeException {

    private String errorCode;
    private String errorDesc;

    public BusinessException(String errorCode, String errorDesc) {
        super(errorDesc);
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode.getCode();
        this.errorDesc = errorCode.getDesc();
    }
}