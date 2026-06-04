package com.iam.auth.engine.authorizer;

import com.iam.auth.dto.request.TokenRequest;
import com.iam.auth.dto.response.TokenResponse;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.TokenException;
import com.nimbusds.jose.JOSEException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuthorizerRegistry {

    private final Map<String, Authorizer> authorizers;

    public AuthorizerRegistry(List<Authorizer> authorizerLst) {
        this.authorizers = new HashMap<>();
        for (Authorizer authorizer: authorizerLst) {
            this.authorizers.put(authorizer.getMethod(), authorizer);
        }
    }

    public TokenResponse issuerToken(TokenRequest request) throws JOSEException {
        if (this.authorizers.containsKey(request.getGrantType())) {
            Authorizer authorizer = this.authorizers.get(request.getGrantType());
            return authorizer.issuerToken(request);
        } else {
            throw new TokenException(ErrorCode.UNSUPPORTED_GRANT_TYPE);
        }
    }
}

