package com.iam.auth.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.auth.domain.AuthSigningKey;
import com.iam.auth.service.BaseService;
import com.iam.auth.service.JwtService;
import com.iam.auth.service.SigningKeyService;
import com.iam.auth.utils.KeyUtility;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl extends BaseService implements JwtService {

    private final SigningKeyService signingKeyService;
    private final ObjectMapper objectMapper;

    @Override
    public String sign(Object claims) throws JOSEException {
        AuthSigningKey key = signingKeyService.getActiveKeyForSigning();
        ECPrivateKey privateKey = signingKeyService.getParsedPrivateKey(key);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(key.getKid())
                .type(JOSEObjectType.JWT)
                .build();

        Map<String, Object> claimsMap = objectMapper.convertValue(claims, new TypeReference<>() {
        });
        JWTClaimsSet claimsSet = buildClaimsSet(claimsMap);

        SignedJWT jwt = new SignedJWT(header, claimsSet);
        jwt.sign(new ECDSASigner(privateKey));
        return jwt.serialize();
    }

    private JWTClaimsSet buildClaimsSet(Map<String, Object> claimsMap) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        claimsMap.forEach((key, value) -> {
            switch (key) {
                case "jti" -> builder.jwtID((String) value);
                case "iss" -> builder.issuer((String) value);
                case "sub" -> builder.subject((String) value);
                case "aud" -> builder.audience((String) value);
                case "iat" -> builder.issueTime(Date.from(Instant.ofEpochSecond(((Number) value).longValue())));
                case "exp" -> builder.expirationTime(Date.from(Instant.ofEpochSecond(((Number) value).longValue())));
                default -> builder.claim(key, value);
            }
        });
        return builder.build();
    }

    @Override
    public Map<String, Object> verify(String token) {
        try {
            if (!this.verifySingature(token))
                return null;
            SignedJWT signedJWT = SignedJWT.parse(token);
            // validate expiration
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (exp != null && exp.before(new Date()))
                return null;
            return signedJWT.getJWTClaimsSet().getClaims();
        } catch (Exception e) {
            log.error("Failed to parse JWT claims: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean verifySignature(String token) {
        return this.verifySingature(token);
    }

    private boolean verifySingature(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String kid = signedJWT.getHeader().getKeyID();
            AuthSigningKey key = signingKeyService.findByKid(kid).orElse(null);
            if (key == null)
                return false;
            ECPublicKey publicKey = KeyUtility.parseEcPublicKey(key.getPublicKey());
            JWSAlgorithm alg = signedJWT.getHeader().getAlgorithm();
            if (!JWSAlgorithm.ES256.equals(alg))
                return false;
            JWSVerifier verifier = new ECDSAVerifier(publicKey);
            return signedJWT.verify(verifier);
        } catch (Exception e) {
            log.error("Failed to verify JWT signature: {}", e.getMessage(), e);
            return false;
        }
    }
}
