package com.iam.auth.engine.authenticator;

import com.iam.auth.engine.AuthSession;
import com.iam.auth.engine.ExecutionResult;
import com.iam.auth.engine.PrepareResult;

import java.util.Map;

public interface Authenticator {
    String getMethod();
    boolean requiredPrepare();
    PrepareResult prepare(AuthSession context);
    ExecutionResult validate(AuthSession context, Map<String, String> payload);
}