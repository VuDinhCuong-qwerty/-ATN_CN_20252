package com.iam.auth.service;

import java.util.Map;

import com.nimbusds.jose.JOSEException;

public interface JwtService {
    String sign(Object claims) throws JOSEException;
    Map<String, Object> verify(String token);
    boolean verifySignature(String token);
}
