package com.iam.auth.service.impl;

import com.iam.auth.domain.AuthApplication;
import com.iam.auth.domain.AuthClient;
import com.iam.auth.domain.AuthUserSession;
import com.iam.auth.dto.pojo.UserAppPermission;
import com.iam.auth.dto.request.LoginRequest;
import com.iam.auth.dto.response.ApiResponse;
import com.iam.auth.dto.response.LoginResponse;
import com.iam.auth.dto.response.LogoutResponse;
import com.iam.auth.dto.response.SelectMethodResponse;
import com.iam.auth.engine.*;
import com.iam.auth.engine.authenticator.Authenticator;
import com.iam.auth.engine.authenticator.AuthenticatorRegistry;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.AuthenticationException;
import com.iam.auth.exception.BusinessException;
import com.iam.auth.repository.jpa.AuthApplicationRepository;
import com.iam.auth.repository.jpa.AuthClientRepository;
import com.iam.auth.repository.jpa.AuthClientSessionRepository;
import com.iam.auth.repository.jpa.AuthRepository;
import com.iam.auth.service.*;
import com.iam.auth.utils.Utility;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends BaseService implements AuthService {

    private static final String LOCK_PREFIX = "auth:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final AuthFlowCache flowCache;
    private final AuthenticatorRegistry authenticatorRegistry;
    private final SSOService ssoService;
    private final AuthSessionService authSessionService;
    private final AuthClientRepository authClientRepository;
    private final AuthApplicationRepository applicationRepository;
    private final AuthRepository authRepository;
    private final AuthClientSessionRepository clientSessionRepository;
    private final RefreshTokenService refreshTokenService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ApiResponse<LoginResponse> login(HttpServletRequest request, HttpServletResponse response,
            LoginRequest input) {
        List<AuthClient> clients = this.authClientRepository.getClientById(input.getClientId());
        if (clients == null || clients.size() != 1) {
            throw new AuthenticationException(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getDesc(), null);
        }
        AuthUserSession userSession = this.ssoService.verifyUserSession(input.getSsoSession(), input.getClientId());
        if (userSession != null) {
            List<AuthApplication> applications = this.applicationRepository.getAppByClientId(input.getClientId(),
                    "ACTIVE");
            if (applications == null || applications.isEmpty()) {
                throw new AuthenticationException(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getDesc(), null);
            }
            return ApiResponse.ok(
                    LoginResponse.builder()
                            .status(LoginResponse.STATUS.TERMINAL_SUCCESS)
                            .redirectUri(applications.getFirst().getDefaultUrl())
                            .build(),
                    request.getRequestURI());
        }
        if (input.getSessionId() == null || input.getSessionId().isBlank()) {
            return handleNewSession(request, response, input, clients.getFirst());
        }
        return handleExistingSession(request, response, input, clients.getFirst());
    }

    // ── 1. handleNewSession ──────────────────────────────────────────────────

    private ApiResponse<LoginResponse> handleNewSession(HttpServletRequest request,
            HttpServletResponse response,
            LoginRequest input, AuthClient client) {
        AuthFlow flow = loadFlow(client.getAppId());
        FlowNode firstNode = flow.getDefaultFirstNode();
        log.info("Input type={}, ndoeType={}", input.getType(), firstNode.getMethod());
        if (!firstNode.getMethod().equals(input.getType().toUpperCase())) {
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(),
                    LoginResponse.builder()
                            .status(LoginResponse.STATUS.TERMINAL_FAIL)
                            .method(input.getType())
                            .clientId(input.getClientId())
                            .theme(firstNode.getTheme())
                            .availableMethods(toMethodInfoList(flow.getEntryPoint().getChildren()))
                            .build());
        }

        AuthSession tempSession = AuthSession.builder()
                // .orgRequestUri(input.getAuthRequestId())
                .clientId(input.getClientId())
                .ssoSession(input.getSsoSession())
                .flowId(flow.getFlowId())
                .currentNodeId(firstNode.getExecutionId())
                .arcLevel(firstNode.getArcLevel())
                .appId(firstNode.getAppId())
                .nodeStatus(new HashMap<>())
                .preparedAt(LocalDateTime.now())
                .build();

        Authenticator authenticator = authenticatorRegistry.get(firstNode.getMethod());
        ExecutionResult result = authenticator.validate(tempSession, input.getPayload());

        if (result.isFailed()) {
            // Rule: INITIAL → FAILED — no session saved
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(),
                    LoginResponse.builder()
                            .status(LoginResponse.STATUS.TERMINAL_FAIL)
                            .method(input.getType())
                            .clientId(input.getClientId())
                            .theme(firstNode.getTheme())
                            .availableMethods(toMethodInfoList(flow.getEntryPoint().getChildren()))
                            .build());
        }

        tempSession.setUserId(result.getUserId());

        if (firstNode.isLeaf()) {
            return handleSuccess(request, response, tempSession);
        }

        // SUCCESS + not leaf → prepare next node, save session
        FlowNode nextNode = firstNode.getDefaultChild();
        PrepareResult prepareResult = prepareNode(nextNode, tempSession);

        tempSession.setSessionId(Utility.generateSessionID());
        tempSession.setPreNodeId(firstNode.getExecutionId());
        tempSession.setCurrentNodeId(nextNode.getExecutionId());
        tempSession.getNodeStatus().put(tempSession.getPreNodeId(), LoginResponse.STATUS.SUCCESS);
        authSessionService.save(tempSession);

        return buildWaitingResponse(
                request.getRequestURI(),
                tempSession.getSessionId(),
                nextNode,
                prepareResult,
                firstNode.getChildren());
    }

    // ── 2. handleExistingSession ─────────────────────────────────────────────

    private ApiResponse<LoginResponse> handleExistingSession(HttpServletRequest request,
            HttpServletResponse response,
            LoginRequest input, AuthClient client) {
        AuthSession session = authSessionService.getSessionById(input.getSessionId()).orElse(null);
        if (session == null) {
            AuthFlow flow = loadFlow(client.getAppId());
            FlowNode firstNode = flow.getDefaultFirstNode();
            AuthSession tempSession = AuthSession.builder().clientId(input.getClientId()).build();
            PrepareResult prepareResult = prepareNode(firstNode, tempSession);
            throw new AuthenticationException(
                    ErrorCode.SESSION_EXPIRED.getCode(), ErrorCode.SESSION_EXPIRED.getDesc(),
                    LoginResponse.builder()
                            .status(LoginResponse.STATUS.TERMINAL_FAIL)
                            .redirectUri(prepareResult.getHint())
                            .build());
        }

        String lockKey = LOCK_PREFIX + session.getSessionId();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new AuthenticationException(ErrorCode.UNKNOWN.getCode(), ErrorCode.UNKNOWN.getDesc(), null);
        }

        try {
            AuthFlow flow = loadFlow(client.getAppId());

            FlowNode curNode = flow.getNode(session.getCurrentNodeId());
            FlowNode preNode = flow.getNode(session.getPreNodeId());
            List<LoginResponse.MethodInfo> siblings = preNode != null
                    ? toMethodInfoList(preNode.getChildren()) : List.of();

            if (curNode == null || !curNode.getMethod().equalsIgnoreCase(input.getType())) {
                throw new AuthenticationException(
                        ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(),
                        LoginResponse.builder()
                                .status(LoginResponse.STATUS.FAIL).sessionId(input.getSessionId())
                                .method(input.getType()).theme(flow.getTheme())
                                .clientId(input.getClientId())
                                .availableMethods(siblings)
                                .build());
            }

            Authenticator authenticator = authenticatorRegistry.get(curNode.getMethod());
            ExecutionResult result = authenticator.validate(session, input.getPayload());

            if (result.isFailed()) {
                // Rule: FAILED is NOT terminal — keep session so user can retry
                throw new AuthenticationException(
                        ErrorCode.UNAUTHORIZED,
                        LoginResponse.builder()
                                .status(LoginResponse.STATUS.FAIL).sessionId(input.getSessionId())
                                .method(input.getType()).theme(curNode.getTheme())
                                .clientId(input.getClientId())
                                .availableMethods(siblings)
                                .build());
            }

            if (curNode.isLeaf()) {
                return handleSuccess(request, response, session);
            }

            // SUCCESS + not leaf → advance to next node
            FlowNode nextNode = curNode.getDefaultChild();
            PrepareResult prepareResult = prepareNode(nextNode, session);

            session.setPreNodeId(curNode.getExecutionId());
            session.setCurrentNodeId(nextNode.getExecutionId());
            session.getNodeStatus().put(session.getPreNodeId(), LoginResponse.STATUS.SUCCESS);
            authSessionService.save(session);

            return buildWaitingResponse(
                    request.getRequestURI(),
                    session.getSessionId(),
                    nextNode,
                    prepareResult,
                    curNode.getChildren());

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    // ── 3. handleSuccess ─────────────────────────────────────────────────────

    private ApiResponse<LoginResponse> handleSuccess(HttpServletRequest request,
            HttpServletResponse response,
            AuthSession session) {

        long userId = session.getUserId();
        long clientId = session.getClientId();
        UserAppPermission userAppPermission = this.authRepository.getUserAppPermit(userId, clientId);
        if (userAppPermission == null) {
            // User không có quyền vào app mà client này thuộc về
            log.warn("[AUTH] User {} không có quyền app {}", userId, clientId);
            throw new AuthenticationException(
                    ErrorCode.FORBIDDEN.getCode(),
                    "User không có quyền truy cập ứng dụng này",
                    LoginResponse.builder()
                            .status(LoginResponse.STATUS.TERMINAL_FAIL)
                            .clientId(clientId)
                            .build());
        }

        // ✓ App access verified → attach to existing SSO session or create new
        String ssoSessionId = ssoService.attachOrCreateSSOSession(session, session.getSsoSession());
        ResponseCookie cookie = ResponseCookie.from("SSO_SESSION", ssoSessionId)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/ms-internal-iam")
                .maxAge(86400)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        authSessionService.delete(session.getSessionId());

        log.info("[AUTH] Login success userId={} clientId={} sessionId={}", userId, clientId, ssoSessionId);
        String orgUri = null;
        if (session.getOrgRequestUri() == null || session.getOrgRequestUri().isBlank()) {
            List<AuthApplication> applications = this.applicationRepository.getAppByClientId(clientId, "ACTIVE");
            if (applications == null || applications.isEmpty()) {
                throw new AuthenticationException(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getDesc(), null);
            }
            orgUri = applications.getFirst().getDefaultUrl();
        } else {
            orgUri = session.getOrgRequestUri();
        }
        return ApiResponse.ok(
                LoginResponse.builder()
                        .status(LoginResponse.STATUS.TERMINAL_SUCCESS)
                        .redirectUri(orgUri)
                        .build(),
                request.getRequestURI());
    }

    // ── 4. prepareNode ───────────────────────────────────────────────────────

    private PrepareResult prepareNode(FlowNode node, AuthSession session) {
        Authenticator authenticator = authenticatorRegistry.get(node.getMethod());
        return authenticator.prepare(session);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ApiResponse<LoginResponse> buildWaitingResponse(String path,
            String sessionId,
            FlowNode nextNode,
            PrepareResult prepareResult,
            List<FlowNode> siblings) {
        return ApiResponse.ok(
                LoginResponse.builder()
                        .status(LoginResponse.STATUS.WAITING)
                        .sessionId(sessionId)
                        .challengeInfo(toChallengeInfo(nextNode, prepareResult))
                        .availableMethods(toMethodInfoList(siblings))
                        .build(),
                path);
    }

    private LoginResponse.ChallengeInfo toChallengeInfo(FlowNode node, PrepareResult prepareResult) {
        return LoginResponse.ChallengeInfo.builder()
                .type(node.getMethod())
                .hint(prepareResult.getHint())
                .build();
    }

    private List<LoginResponse.MethodInfo> toMethodInfoList(List<FlowNode> nodes) {
        if (nodes == null)
            return List.of();
        return nodes.stream()
                .map(n -> LoginResponse.MethodInfo.builder()
                        .type(n.getMethod())
                        .nodeId(n.getExecutionId())
                        .isDefault(n.isDefault())
                        .label(methodLabel(n.getMethod()))
                        .build())
                .toList();
    }

    private String methodLabel(String method) {
        return switch (method) {
            case "USERNAME_PASSWORD" -> "Mật khẩu";
            case "OTP_EMAIL"         -> "OTP Email";
            default                  -> method;
        };
    }

    private AuthFlow loadFlow(Long appId) {
        try {
            AuthFlow flow = flowCache.getByAppId(appId);
            if (flow == null) {
                throw new AuthenticationException(
                        ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(),
                        LoginResponse.builder().status(LoginResponse.STATUS.FAIL).build());
            }
            return flow;
        } catch (BusinessException e) {
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(),
                    LoginResponse.builder().status(LoginResponse.STATUS.FAIL).build());
        }
    }

    // ── Switch method ────────────────────────────────────────────────────────

    @Override
    public SelectMethodResponse getSelectableMethods(String authSessionId) {
        AuthSession session = authSessionService.getSessionById(authSessionId).orElse(null);
        if (session == null) {
            throw new AuthenticationException(
                    ErrorCode.SESSION_EXPIRED.getCode(), ErrorCode.SESSION_EXPIRED.getDesc(), null);
        }

        AuthFlow flow = loadFlow(session.getAppId());
        FlowNode currentNode = flow.getNode(session.getCurrentNodeId());
        FlowNode parentNode  = flow.getNode(session.getPreNodeId());

        List<FlowNode> siblings = parentNode != null ? parentNode.getChildren() : List.of();

        List<SelectMethodResponse.MethodOption> methods = siblings.stream()
                .map(n -> SelectMethodResponse.MethodOption.builder()
                        .nodeId(n.getExecutionId())
                        .type(n.getMethod())
                        .label(methodLabel(n.getMethod()))
                        .build())
                .toList();

        return SelectMethodResponse.builder()
                .clientId(session.getClientId())
                .currentMethodType(currentNode != null ? currentNode.getMethod() : null)
                .methods(methods)
                .build();
    }

    @Override
    public LoginResponse doSwitchMethod(String authSessionId, Long nodeId) {
        AuthSession session = authSessionService.getSessionById(authSessionId).orElse(null);
        if (session == null) {
            throw new AuthenticationException(
                    ErrorCode.SESSION_EXPIRED.getCode(), ErrorCode.SESSION_EXPIRED.getDesc(), null);
        }

        String lockKey = LOCK_PREFIX + session.getSessionId();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new AuthenticationException(ErrorCode.UNKNOWN.getCode(), ErrorCode.UNKNOWN.getDesc(), null);
        }

        try {
            AuthFlow flow = loadFlow(session.getAppId());
            FlowNode parentNode = flow.getNode(session.getPreNodeId());
            if (parentNode == null) {
                throw new AuthenticationException(
                        ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(), null);
            }

            FlowNode selectedNode = parentNode.getChildren().stream()
                    .filter(n -> n.getExecutionId().equals(nodeId))
                    .findFirst()
                    .orElse(null);

            if (selectedNode == null || selectedNode.getExecutionId().equals(session.getCurrentNodeId())) {
                throw new AuthenticationException(
                        ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(), null);
            }

            PrepareResult prepareResult = prepareNode(selectedNode, session);

            session.setCurrentNodeId(selectedNode.getExecutionId());
            authSessionService.save(session);

            return LoginResponse.builder()
                    .status(LoginResponse.STATUS.WAITING)
                    .sessionId(authSessionId)
                    .clientId(session.getClientId())
                    .challengeInfo(toChallengeInfo(selectedNode, prepareResult))
                    .availableMethods(toMethodInfoList(parentNode.getChildren()))
                    .build();

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public ApiResponse<LogoutResponse> logout(String ssoSession, String bearerToken) {
        log.info("Logout user {}", ssoSession);
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED.getCode(),
                    "Missing or invalid Authorization header",
                    LogoutResponse.builder().status(LogoutResponse.STATUS.FAIL).build());
        }

        Map<String, Object> claims;
        try {
            SignedJWT jwt = SignedJWT.parse(bearerToken.substring(7));
            claims = jwt.getJWTClaimsSet().getClaims();
        } catch (Exception e) {
            log.warn("[LOGOUT] Failed to parse token: {}", e.getMessage());
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED.getCode(), "Invalid token format",
                    LogoutResponse.builder().status(LogoutResponse.STATUS.FAIL).build());
        }

        Long appId    = claims.get("appId") != null ? ((Number) claims.get("appId")).longValue() : null;
        String clientId = (String) claims.get("clientId");
        Long userId     = parseSubjectAsLong(claims.get("sub"));

        if (ssoSession != null && !ssoSession.isBlank()) {
            // ① Revoke the SSO session itself (AUTH_USER_SESSION.STATUS = 0)
            ssoService.revokeSession(ssoSession);
            log.info("[LOGOUT] SSO session revoked: sessionId={}", ssoSession);

            // ② Revoke client session for this app
            if (appId != null) {
                clientSessionRepository.invalidateBySessionIdAndAppId(ssoSession, appId);
                log.info("[LOGOUT] client session invalidated: sessionId={}, appId={}", ssoSession, appId);
            }

            // ③ Revoke refresh tokens for this session
            if (clientId != null) {
                refreshTokenService.revokeBySession(ssoSession, clientId);
                log.info("[LOGOUT] refresh tokens revoked: sessionId={}, clientId={}", ssoSession, clientId);
            }
        } else {
            // Cookie not received — fall back to revoking ALL sessions for this user
            log.warn("[LOGOUT] SSO_SESSION cookie missing, falling back to full revocation for userId={}", userId);
            if (userId != null) {
                ssoService.revokeAllSessionsByUserId(userId);
                log.info("[LOGOUT] All SSO sessions revoked for userId={}", userId);
            }
        }

        return ApiResponse.ok(LogoutResponse.builder().status(LogoutResponse.STATUS.SUCCESS).build(), "/logout");
    }

    private Long parseSubjectAsLong(Object sub) {
        if (sub == null) return null;
        if (sub instanceof Number n) return n.longValue();
        try { return Long.parseLong(sub.toString()); } catch (NumberFormatException e) { return null; }
    }
}
