package com.iam.auth.service;

import com.iam.auth.domain.AuthSigningKey;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Optional;

public interface SigningKeyService {
    AuthSigningKey generateAndSaveKey();
    void rotateKey();
    Optional<AuthSigningKey> findActiveKey();
    void disableExpiredKeys();
    List<AuthSigningKey> findAllVerifiableKeys();
    AuthSigningKey getActiveKeyForSigning();
    ECPrivateKey getParsedPrivateKey(AuthSigningKey signingKey);
    ECPublicKey getParsedPublicKey(AuthSigningKey signingKey);
    Optional<AuthSigningKey> findByKid(String kid);
}
