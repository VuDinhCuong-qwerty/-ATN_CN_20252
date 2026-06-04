package com.iam.auth.engine.authenticator;

import com.iam.auth.config.AuthProperties;
import com.iam.auth.domain.AuthMethod;
import com.iam.auth.domain.AuthUser;
import com.iam.auth.dto.request.LoginRequest;
import com.iam.auth.engine.AuthSession;
import com.iam.auth.engine.ExecutionResult;
import com.iam.auth.engine.PrepareResult;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.repository.jpa.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserNameAuthenticator implements Authenticator{

    private final AuthProperties authProperties;
    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String getMethod() {
        return AuthMethod.METHOD.USERNAME_PASSWORD;
    }

    @Override
    public boolean requiredPrepare() {
        return false;
    }

    @Override
    public PrepareResult prepare(AuthSession context) {
        String hint = authProperties.getEndpoints().getLoginPage() +
                "?client-id=" + context.getClientId() +
                "&action-type=" + AuthMethod.METHOD.USERNAME_PASSWORD.toLowerCase() +
                "&theme=" + "default";
        return PrepareResult.builder()
                .methodType(AuthMethod.METHOD.USERNAME_PASSWORD)
                .hint(hint)
                .build();
    }

    @Override
    public ExecutionResult validate(AuthSession session, Map<String, String> payload) {
        String username = payload.getOrDefault(LoginRequest.PayloadHeader.USERNAME, null);
        String password = payload.getOrDefault(LoginRequest.PayloadHeader.PASSWORD, null);
        if (session.getNodeStatus() == null) {
            session.setNodeStatus(new HashMap<>());
        }
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            session.getNodeStatus().put(session.getCurrentNodeId(), "FAIL");
            log.info("Username or Passwword is blank!");
            return ExecutionResult.failed(ErrorCode.INVALID_CREDENTIALS.getCode(), ErrorCode.INVALID_CREDENTIALS.getCode());
        }

        AuthUser user = this.userRepository.findByUsername(username.toUpperCase()).orElse(null);
        if (user == null || !this.passwordEncoder.matches(password, user.getPassword()) || !"ACTIVE".equals(user.getStatus())) {
            session.getNodeStatus().put(session.getCurrentNodeId(), "FAIL");
            log.info("Username {} or Passwword {} is invalid!", username, password);
            return ExecutionResult.failed(ErrorCode.INVALID_CREDENTIALS.getCode(), ErrorCode.INVALID_CREDENTIALS.getCode());
        }

        session.getNodeStatus().put(session.getCurrentNodeId(), "SUCCESS");
        session.setUserId(user.getId());
        return ExecutionResult.builder()
                .status(ExecutionResult.Status.SUCCESS)
                .userId(session.getUserId())
                .build();
    }
}
