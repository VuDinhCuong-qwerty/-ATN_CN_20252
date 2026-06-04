package com.iam.auth.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class Utility {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String generateSessionID() {
        return "ias_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateAuthorizationCode() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String toJson(Object object) {
        if (object == null) return "null";
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return object.toString();
        }
    }

}
