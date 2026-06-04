package com.demo.change.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Utility {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toJson(Object object) {
        if (object == null) return "null";
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            return object.toString();
        }
    }
}
