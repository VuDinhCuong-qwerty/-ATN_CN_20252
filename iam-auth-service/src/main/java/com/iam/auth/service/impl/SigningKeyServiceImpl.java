package com.iam.auth.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.iam.auth.domain.AuthSigningKey;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.BusinessException;
import com.iam.auth.repository.jpa.AuthSigningKeyRepository;
import com.iam.auth.service.BaseService;
import com.iam.auth.service.SigningKeyService;
import com.iam.auth.utils.KeyUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SigningKeyServiceImpl extends BaseService implements SigningKeyService {

    private final AuthSigningKeyRepository authSigningKeyRepository;
    private final Cache<String, ECPrivateKey> privateKeyCache  = Caffeine.newBuilder()
            .expireAfterWrite(100, TimeUnit.DAYS).maximumSize(5)
            .recordStats().build();
    private final Cache<String, ECPublicKey> publicKeyCache  = Caffeine.newBuilder()
            .expireAfterWrite(100, TimeUnit.DAYS).maximumSize(5)
            .recordStats().build();

    @Override
    @Transactional
    public AuthSigningKey generateAndSaveKey() {
        log.info("Generating new ES256 key pair");

        KeyPair keyPair = generateEcKeyPair();
        String privateKeyPem = KeyUtility.toPemPrivateKey(keyPair.getPrivate().getEncoded());
        String publicKeyPem = KeyUtility.toPemPublicKey(keyPair.getPublic().getEncoded());

        LocalDateTime validUntil = LocalDateTime.now()
                .plusDays(120)
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        AuthSigningKey newKey = AuthSigningKey.builder()
                .kid(UUID.randomUUID().toString())
                .algorithm(AuthSigningKey.Algorithm.ES256.name())
                .publicKey(publicKeyPem)
                .privateKey(privateKeyPem)
                .status(AuthSigningKey.Status.ACTIVE.name())
                .validUntil(validUntil)
                .build();

        // INSERT INTO AUTH_SIGNING_KEY (KID, ALGORITHM, PUBLIC_KEY, PRIVATE_KEY, STATUS, VALID_UNTIL, VALID_FROM, CREATED_AT) VALUES (...)
        AuthSigningKey saved = authSigningKeyRepository.save(newKey);
        log.info("New ACTIVE key saved with kid={}, validUntil={}", saved.getKid(), saved.getValidUntil());
        return saved;
    }

    @Override
    @Transactional
    public void rotateKey() {
        log.info("Starting key rotation");

        // SELECT * FROM AUTH_SIGNING_KEY WHERE STATUS = 'ACTIVE' ORDER BY VALID_FROM DESC FETCH FIRST 1 ROWS ONLY
        Optional<AuthSigningKey> currentActiveOpt = authSigningKeyRepository.findFirstByStatus(
                AuthSigningKey.Status.ACTIVE.name());

        // Step 1: Generate and save new ACTIVE key (ensures no signing gap)
        log.info("Step 1: Generating new ACTIVE key");
        AuthSigningKey newKey = generateAndSaveKey();
        log.info("New ACTIVE key created with kid={}", newKey.getKid());

        // Step 2: Set old ACTIVE key to PASSIVE
        if (currentActiveOpt.isPresent()) {
            AuthSigningKey oldKey = currentActiveOpt.get();
            log.info("Step 2: Setting old ACTIVE key kid={} to PASSIVE", oldKey.getKid());
            oldKey.setStatus(AuthSigningKey.Status.PASSIVE.name());
            // UPDATE AUTH_SIGNING_KEY SET STATUS = 'PASSIVE' WHERE ID = :id (via save)
            authSigningKeyRepository.save(oldKey);
            log.info("Old key kid={} is now PASSIVE", oldKey.getKid());
        } else {
            log.info("Step 2: No existing ACTIVE key found — no key to demote");
        }

        log.info("Key rotation complete. New ACTIVE kid={}", newKey.getKid());
    }

    @Override
    public Optional<AuthSigningKey> findActiveKey() {
        // SELECT * FROM AUTH_SIGNING_KEY WHERE STATUS = 'ACTIVE' ORDER BY VALID_FROM DESC FETCH FIRST 1 ROWS ONLY
        return authSigningKeyRepository.findFirstByStatus(AuthSigningKey.Status.ACTIVE.name());
    }

    @Override
    @Transactional
    public void disableExpiredKeys() {
        log.info("Disabling expired PASSIVE keys");

        // SELECT * FROM AUTH_SIGNING_KEY WHERE STATUS = 'PASSIVE' AND VALID_UNTIL <= CURRENT_TIMESTAMP
        List<AuthSigningKey> expiredPassiveKeys = authSigningKeyRepository.findByStatusAndValidUntilBefore(
                AuthSigningKey.Status.PASSIVE.name(), LocalDateTime.now());

        if (expiredPassiveKeys.isEmpty()) {
            log.info("No expired PASSIVE keys found");
            return;
        }

        expiredPassiveKeys.forEach(key -> {
            key.setStatus(AuthSigningKey.Status.DISABLED.name());
            // UPDATE AUTH_SIGNING_KEY SET STATUS = 'DISABLED' WHERE ID = :id (via save)
            authSigningKeyRepository.save(key);
            log.info("Key kid={} disabled (was PASSIVE, validUntil={})", key.getKid(), key.getValidUntil());
        });

        log.info("Disabled {} expired PASSIVE key(s)", expiredPassiveKeys.size());
    }

    @Override
    public List<AuthSigningKey> findAllVerifiableKeys() {
        // SELECT * FROM AUTH_SIGNING_KEY WHERE STATUS != 'DISABLED'
        return authSigningKeyRepository.findByStatusNot(AuthSigningKey.Status.DISABLED.name());
    }

    @Override
    @Transactional
    public AuthSigningKey getActiveKeyForSigning() {
        // SELECT * FROM AUTH_SIGNING_KEY WHERE STATUS = 'ACTIVE' ORDER BY VALID_FROM DESC FETCH FIRST 1 ROWS ONLY
        return authSigningKeyRepository.findFirstByStatus(AuthSigningKey.Status.ACTIVE.name())
                .orElseGet(() -> {
                    log.warn("No ACTIVE signing key found — generating a new one");
                    return generateAndSaveKey();
                });
    }

    @Override
    public ECPrivateKey getParsedPrivateKey(AuthSigningKey signingKey) {
        return this.privateKeyCache.get(signingKey.getKid(),
                k -> KeyUtility.parseECPrivateKey(signingKey.getPrivateKey()));
    }

    @Override
    public ECPublicKey getParsedPublicKey(AuthSigningKey signingKey) {
        return this.publicKeyCache.get(signingKey.getKid(),
                k -> {
                    try {
                        return KeyUtility.parseEcPublicKey(signingKey.getPublicKey());
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public Optional<AuthSigningKey> findByKid(String kid) {
        // SELECT * FROM AUTH_SIGNING_KEY WHERE KID = :kid
        return authSigningKeyRepository.findByKid(kid);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private KeyPair generateEcKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            keyPairGenerator.initialize(ecSpec, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | java.security.InvalidAlgorithmParameterException e) {
            log.error("Failed to generate EC key pair", e);
            throw new BusinessException(ErrorCode.SIGNING_KEY_GENERATION_FAILED, e.getMessage());
        }
    }
}
