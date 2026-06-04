package com.iam.auth.enums;

public final class Constant {
    public interface TOKEN_ENDPOINT_AUTH {
        String CLIENT_SECRET_BASIC = "client_secret_basic";
        String CLIENT_SECRET_POST  = "client_secret_post";
        String CLIENT_SECRET_JWT   = "client_secret_jwt";
        String PRIVATE_KEY_JWT     = "private_key_jwt";
        String NONE                = "none";
    }

    public interface GRANT_TYPE {
        String AUTHORIZATION_CODE = "authorization_code";
        String CLIENT_CREDENTIALS = "client_credentials";
        String REFRESH_TOKEN       = "refresh_token";
        String IMPLICIT            = "implicit";           // deprecated OAuth 2.0
        String PASSWORD            = "password";           // deprecated OAuth 2.0
    }
}
