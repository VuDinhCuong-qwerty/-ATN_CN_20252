package com.iam.auth.service;

import com.iam.auth.domain.AuthUserSession;
import com.iam.auth.dto.pojo.AuthCode;
import com.iam.auth.dto.pojo.Client;
import com.iam.auth.dto.request.AuthorizeRequest;

public interface AuthorizationCodeService {
    String createAuthorizationCode(AuthorizeRequest request, Client client, AuthUserSession session);
    AuthCode getAuthorizationCode(String code);
    boolean markCodeUsed(String code);
    void revokeTokensByCode(String code);
    boolean isUsed(String code);
}
