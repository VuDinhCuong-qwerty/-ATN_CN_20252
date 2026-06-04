package com.iam.auth.controller;


import com.iam.auth.config.AuthProperties;
import com.iam.auth.dto.request.AuthorizeRequest;
import com.iam.auth.dto.request.LoginRequest;
import com.iam.auth.dto.request.SwitchMethodConfirmRequest;
import com.iam.auth.dto.request.SwitchMethodInitRequest;
import com.iam.auth.dto.response.ApiResponse;
import com.iam.auth.dto.response.AuthorizeResponse;
import com.iam.auth.dto.response.LoginResponse;
import com.iam.auth.dto.response.LogoutResponse;
import com.iam.auth.dto.response.SelectMethodResponse;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.AuthenticationException;
import com.iam.auth.service.AuthService;
import com.iam.auth.service.OAuth2Service;
import com.iam.auth.utils.SessionTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ms-internal-iam/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuth2Service oAuth2Service;
    private final String tokenSecret;

    public AuthController(AuthService authService,
                          OAuth2Service oAuth2Service,
                          AuthProperties authProperties,
                          @Value("${session.token.secret}") String tokenSecret) {
        this.authService = authService;
        this.oAuth2Service = oAuth2Service;
        this.tokenSecret = tokenSecret;
    }

    @GetMapping("/authorize")
    public String authorize(
            @RequestParam("client_id")                                      String clientId,
            @RequestParam("redirect_uri")                                   String redirectUri,
            @RequestParam("response_type")                                  String responseType,
            @RequestParam("state")                                          String state,
            @RequestParam(value = "scope",                 required = false) String scope,
            @RequestParam(value = "code_challenge",        required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            @RequestParam(value = "nonce",                 required = false) String nonce,
            @CookieValue(value = "SSO_SESSION",            required = false) String ssoSession,
            HttpServletResponse response, HttpSession session,
            Model model
    ) throws IOException {

        List<String> scopes = (scope != null && !scope.isBlank())
                ? Arrays.asList(scope.trim().split("\\s+"))
                : Collections.emptyList();

        AuthorizeRequest request = AuthorizeRequest.builder()
                .clientId(clientId)
                .redirectUri(redirectUri)
                .responseType(responseType)
                .state(state)
                .scopes(scopes)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .nonce(nonce)
                .ssoSession(ssoSession)
                .build();
        AuthorizeResponse authorizeResponse = oAuth2Service.authorize(request, response, session);

        if (AuthorizeResponse.STATUS.OK.equals(authorizeResponse.getStatus())) {
            return "redirect:" + authorizeResponse.getRedirectUri();  // external redirect
        } else if (AuthorizeResponse.STATUS.UNAUTHENTICATED.equals(authorizeResponse.getStatus())) {
            return "redirect:" + authorizeResponse.getRedirectUri(); // internal redirect
        } else {
            model.addAttribute("errorImg", ErrorCode.BAD_REQUEST_PAGE.getDesc());
            return authorizeResponse.getRedirectUri();  // internal redirect
        }
    }

    /**
     * Handles every MFA step POST.
     *
     * <p>The HTML form submits {@code sessionToken} — an HMAC-bound derivative of the real
     * {@code authSessionId}. The controller resolves the real ID from HttpSession, verifies the
     * HMAC, injects the ID into the request, then discards the token (single-use). A fresh token
     * is generated on the next page render by {@link PageController}.
     */
    @PostMapping("/login")
    public String login(HttpServletRequest httpServletRequest,
                        HttpServletResponse httpServletResponse,
                        @ModelAttribute LoginRequest input,
                        @RequestParam(name = "sessionToken", required = false) String sessionToken,
                        @CookieValue(value = "SSO_SESSION", required = false) String ssoSession,
                        HttpSession session) {

        // Resolve sessionToken → real authSessionId (null for the very first step)
        String authSessionId = resolveAndInvalidateToken(session, sessionToken, input);
        input.setSessionId(authSessionId);
        input.setSsoSession(ssoSession);

        ApiResponse<LoginResponse> response = this.authService.login(httpServletRequest, httpServletResponse, input);
        LoginResponse loginResponse = response.getData();

        String status = loginResponse.getStatus();
        if (LoginResponse.STATUS.TERMINAL_SUCCESS.equals(status) || LoginResponse.STATUS.TERMINAL_FAIL.equals(status)) {
            session.removeAttribute("auth:ui:context");
            return "redirect:" + loginResponse.getRedirectUri();
        } else {
            storeAuthSession(session, loginResponse);
            return "redirect:" + loginResponse.getChallengeInfo().getHint();
        }
    }

    // ── Token resolution ──────────────────────────────────────────────────────

    private String resolveAndInvalidateToken(HttpSession session, String sessionToken, LoginRequest input) {
        if (sessionToken == null || sessionToken.isBlank()) {
            // First step — no MFA session established yet
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) session.getAttribute("auth:ui:context");
        if (data == null) {
            throw buildTokenError(input);
        }

        String authSessionId   = (String) data.get("authSessionId");
        String storedToken     = (String) data.get("sessionToken");
        Long   tokenTimestamp  = (Long)   data.get("tokenTimestamp");

        if (authSessionId == null || storedToken == null || tokenTimestamp == null) {
            throw buildTokenError(input);
        }

        // Recompute HMAC using the same inputs that were used at generation time
        String jsessionId = session.getId();
        String expected = SessionTokenUtil.generate(tokenSecret, authSessionId, jsessionId, tokenTimestamp);

        if (!SessionTokenUtil.verify(expected, sessionToken)) {
            throw buildTokenError(input);
        }

        // Single-use: discard immediately so the token cannot be replayed
        data.remove("sessionToken");
        data.remove("tokenTimestamp");
        session.setAttribute("auth:ui:context", data);

        return authSessionId;
    }

    private AuthenticationException buildTokenError(LoginRequest input) {
        return new AuthenticationException(
                ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(),
                LoginResponse.builder()
                        .status(LoginResponse.STATUS.TERMINAL_FAIL)
                        .clientId(input.getClientId())
                        .method(input.getType())
                        .theme("default")
                        .build()
        );
    }

    // ── HttpSession management ────────────────────────────────────────────────

    /**
     * Stores the auth context in HttpSession after a successful intermediate step.
     * Note: {@code authSessionId} is stored under the key {@code "authSessionId"} and is
     * never written into the Thymeleaf model. Only a derived {@code sessionToken} reaches HTML
     * (generated by {@link PageController} on the next GET).
     */
    private void storeAuthSession(HttpSession session, LoginResponse loginResponse) {
        Map<String, Object> data = new HashMap<>();
        data.put("authSessionId",    loginResponse.getSessionId());   // never sent to browser
        data.put("clientId",         loginResponse.getClientId());
        data.put("availableMethods", loginResponse.getAvailableMethods());
        data.put("challengeInfo",    loginResponse.getChallengeInfo());
        // sessionToken + tokenTimestamp are added by PageController on the GET that follows
        session.setAttribute("auth:ui:context", data);
    }

    // ── Switch method ─────────────────────────────────────────────────────────

    /**
     * Step 1: validate the sessionToken from the current login page and render the
     * select-method page. The old token is consumed here; a new single-use token is
     * stored in HttpSession and passed to the template.
     */
    @PostMapping("/switch-method")
    public String switchMethodInit(
            @ModelAttribute SwitchMethodInitRequest input,
            HttpSession session,
            Model model) {

        String authSessionId = resolveTokenForSwitch(session, input.getSessionToken());

        SelectMethodResponse data = authService.getSelectableMethods(authSessionId);

        long tokenTimestamp = System.currentTimeMillis();
        String newToken = SessionTokenUtil.generate(tokenSecret, authSessionId, session.getId(), tokenTimestamp);

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) session.getAttribute("auth:ui:context");
        if (ctx == null) ctx = new HashMap<>();
        ctx.put("sessionToken",    newToken);
        ctx.put("tokenTimestamp",  tokenTimestamp);
        session.setAttribute("auth:ui:context", ctx);

        model.addAttribute("clientId",           data.getClientId());
        model.addAttribute("sessionToken",        newToken);
        model.addAttribute("currentMethodType",   data.getCurrentMethodType());
        model.addAttribute("methods",             data.getMethods());

        return "default/select_method/index";
    }

    /**
     * Step 2: user picked a method — switch the active node, then redirect to
     * the new method's login page (PageController will render it with a fresh token).
     */
    @PostMapping("/switch-method/confirm")
    public String switchMethodConfirm(
            @ModelAttribute SwitchMethodConfirmRequest input,
            HttpSession session) {

        String authSessionId = resolveTokenForSwitch(session, input.getSessionToken());

        LoginResponse loginResponse = authService.doSwitchMethod(authSessionId, input.getNodeId());

        storeAuthSession(session, loginResponse);
        return "redirect:" + loginResponse.getChallengeInfo().getHint();
    }

    /**
     * Validates the HMAC sessionToken for the switch-method flow.
     * Throws (null data) on any failure — GlobalExceptionHandler redirects to error page.
     */
    private String resolveTokenForSwitch(HttpSession session, String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(), null);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) session.getAttribute("auth:ui:context");
        if (ctx == null) {
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(), null);
        }

        String authSessionId    = (String) ctx.get("authSessionId");
        String storedToken      = (String) ctx.get("sessionToken");
        Long   tokenTimestamp   = (Long)   ctx.get("tokenTimestamp");

        if (authSessionId == null || storedToken == null || tokenTimestamp == null) {
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(), null);
        }

        String expected = SessionTokenUtil.generate(tokenSecret, authSessionId, session.getId(), tokenTimestamp);
        if (!SessionTokenUtil.verify(expected, sessionToken)) {
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDesc(), null);
        }

        ctx.remove("sessionToken");
        ctx.remove("tokenTimestamp");
        session.setAttribute("auth:ui:context", ctx);

        return authSessionId;
    }

    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @CookieValue(value = "SSO_SESSION", required = false) String ssoSession) {

        ApiResponse<LogoutResponse> response = this.authService.logout(ssoSession, authorization);
        return ResponseEntity.ok(response);
    }
}