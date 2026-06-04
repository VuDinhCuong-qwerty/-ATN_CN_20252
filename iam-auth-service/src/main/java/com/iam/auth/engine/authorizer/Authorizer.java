package com.iam.auth.engine.authorizer;

import com.iam.auth.dto.request.TokenRequest;
import com.iam.auth.dto.response.TokenResponse;
import com.nimbusds.jose.JOSEException;

public interface Authorizer {
    String getMethod();
    TokenResponse issuerToken(TokenRequest input) throws JOSEException;
}
