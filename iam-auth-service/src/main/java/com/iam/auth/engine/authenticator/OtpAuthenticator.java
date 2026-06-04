package com.iam.auth.engine.authenticator;

import com.iam.auth.config.AuthProperties;
import com.iam.auth.domain.AuthMethod;
import com.iam.auth.dto.request.LoginRequest;
import com.iam.auth.engine.AuthSession;
import com.iam.auth.engine.ExecutionResult;
import com.iam.auth.engine.PrepareResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OtpAuthenticator implements Authenticator {

    private final AuthProperties authProperties;

    @Override
    public String getMethod() {
        return AuthMethod.METHOD.OTP_EMAIL;
    }

    @Override
    public boolean requiredPrepare() {
        return true;
    }

    @Override
    public PrepareResult prepare(AuthSession context) {
        // gen otp send to email (đã đăng ký trước đó)
        String hint = authProperties.getEndpoints().getLoginPage()
                + "?client-id=" + context.getClientId()
                + "&action-type=" + AuthMethod.METHOD.OTP_EMAIL.toLowerCase()
                + "&theme=default";
        return PrepareResult.builder()
                .methodType(AuthMethod.METHOD.OTP_EMAIL)
                .hint(hint)
                .build();
    }

    @Override
    public ExecutionResult validate(AuthSession session, Map<String, String> payload) {
        String otpValue = payload.getOrDefault(LoginRequest.PayloadHeader.OTP, null);

        if (session.getNodeStatus() == null) {
            session.setNodeStatus(new HashMap<>());
        }
        if (otpValue == null || otpValue.isBlank() || !otpValue.equals("12345678")) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.FAILED)
                    .build();
        }
        return ExecutionResult.builder()
                .status(ExecutionResult.Status.SUCCESS)
                .build();
    }
}
