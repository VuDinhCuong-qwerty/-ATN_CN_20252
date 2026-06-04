package com.iam.auth.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility for generating and verifying single-use session tokens.
 *
 * <p>Token = HMAC-SHA256(SERVER_SECRET, authSessionId + ":" + jsessionId + ":" + timestamp)
 *
 * <p>The authSessionId (Redis key) is kept entirely inside HttpSession and never leaves
 * the server. Only the HMAC derivative is exposed in the HTML hidden field, so stealing
 * the token from HTML is useless without the corresponding JSESSIONID cookie.
 */
public final class SessionTokenUtil {

    private static final String ALGORITHM = "HmacSHA256";

    private SessionTokenUtil() {}

    /**
     * Generates a session token.
     *
     * @param secret        server-side HMAC secret (from application.properties)
     * @param authSessionId the real Redis session key — never sent to the browser
     * @param jsessionId    the Servlet session ID (HttpSession.getId())
     * @param timestamp     epoch-millis at generation time (stored in HttpSession for replay-bound TTL)
     * @return Base64url-encoded HMAC token
     */
    public static String generate(String secret, String authSessionId, String jsessionId, long timestamp) {
        String message = authSessionId + ":" + jsessionId + ":" + timestamp;
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to generate session token", e);
        }
    }

    /**
     * Constant-time comparison of the expected token against the value submitted by the client.
     *
     * @param expected recomputed HMAC on the server
     * @param actual   value submitted from the HTML form
     * @return true only if both values are non-null and byte-equal
     */
    public static boolean verify(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
