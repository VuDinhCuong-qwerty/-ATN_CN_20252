package com.demo.change.exception;

import com.demo.change.constant.ErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final String errorDesc;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDesc());
        this.errorCode = errorCode.getCode();
        this.errorDesc = errorCode.getDesc();
    }

    public BusinessException(ErrorCode errorCode, String errorDesc) {
        super(errorDesc);
        this.errorCode = errorCode.getCode();
        this.errorDesc = errorDesc;
    }

    public BusinessException(String errorCode, String errorDesc) {
        super(errorDesc);
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }
}
