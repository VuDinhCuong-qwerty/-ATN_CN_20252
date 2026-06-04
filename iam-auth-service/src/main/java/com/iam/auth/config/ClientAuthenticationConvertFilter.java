package com.iam.auth.config;

import com.iam.auth.enums.Constant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Order(1)
public class ClientAuthenticationConvertFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!request.getRequestURI().endsWith("/token")) {
            chain.doFilter(request, response);
            return;
        }
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        String authMethod = detectAuthMethod(mutableRequest);
        switch (authMethod) {
            case Constant.TOKEN_ENDPOINT_AUTH.CLIENT_SECRET_BASIC -> convertAuthBasic(mutableRequest);
            case Constant.TOKEN_ENDPOINT_AUTH.PRIVATE_KEY_JWT -> convertJwt(mutableRequest);
            case Constant.TOKEN_ENDPOINT_AUTH.NONE -> convertNone(mutableRequest);
            case Constant.TOKEN_ENDPOINT_AUTH.CLIENT_SECRET_POST -> convertAuthPost(mutableRequest);
        }
        chain.doFilter(mutableRequest, response);
    }

    private void convertAuthPost(MutableHttpServletRequest mutableRequest) {
        //
    }

    private void convertNone(MutableHttpServletRequest mutableRequest) {

    }

    private void convertJwt(MutableHttpServletRequest mutableRequest) {

    }

    private void convertAuthBasic(MutableHttpServletRequest mutableRequest) {
        String authHeader = mutableRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Basic ")) {
            return;
        }

        try {
            String base64  = authHeader.substring("Basic ".length()).trim();
            String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);

            if (parts.length == 2) {
                mutableRequest.setParameter("client_id",     URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                mutableRequest.setParameter("client_secret", URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // decode lỗi → bỏ qua, để controller validate
        }
    }

    private String detectAuthMethod(MutableHttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Basic ")) {
            return Constant.TOKEN_ENDPOINT_AUTH.CLIENT_SECRET_BASIC;
        }
        if (StringUtils.hasText(request.getParameter("client_assertion_type"))) {
            return Constant.TOKEN_ENDPOINT_AUTH.PRIVATE_KEY_JWT;
        }
        if (StringUtils.hasText(request.getParameter("code_verifier"))) {
            return Constant.TOKEN_ENDPOINT_AUTH.NONE;
        }
        return Constant.TOKEN_ENDPOINT_AUTH.CLIENT_SECRET_POST;
    }
}
