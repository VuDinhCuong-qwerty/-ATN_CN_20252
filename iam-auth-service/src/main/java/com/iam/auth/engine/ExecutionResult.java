package com.iam.auth.engine;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExecutionResult {

    private Status status;
    private Long userId;
    private String errorCode;
    private String errorDesc;

    public enum Status {
        SUCCESS,
        FAILED,
    }

    public static ExecutionResult success() {
        return ExecutionResult.builder().status(Status.SUCCESS).build();
    }

    public static ExecutionResult failed(String errorCode, String errorDesc) {
        return ExecutionResult.builder()
                .status(Status.FAILED)
                .errorCode(errorCode)
                .errorDesc(errorDesc)
                .build();
    }


    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isFailed()  { return status == Status.FAILED;  }
}
