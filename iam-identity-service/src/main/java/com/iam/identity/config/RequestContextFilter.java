package com.iam.identity.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.iam.identity.config.context.RequestContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component("gatewayHeaderFilter")
public class RequestContextFilter extends OncePerRequestFilter {

    private final String EMPLOYEE_CODE_HEADER = "X-Employee-Code";
    private final String USERNAME_HEADER = "X-Username";
    private final String USER_ID_HEADER = "X-User-Id";
    private final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            RequestContext.set(
                    request.getHeader(USER_ID_HEADER),
                    request.getHeader(EMPLOYEE_CODE_HEADER),
                    request.getHeader(USERNAME_HEADER),
                    request.getHeader(USER_ROLE_HEADER));
            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }

}
