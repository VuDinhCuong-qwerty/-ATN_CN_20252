package com.iam.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {
    private final Map<String, String[]> customParams = new HashMap<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void setParameter(String name, String value) {
        this.customParams.put(name, new String[]{value});
    }

    @Override
    public String getParameter(String name) {
        if (this.customParams.containsKey(name)) {
            return this.customParams.get(name)[0];
        }
        return super.getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> merged = new HashMap<>(super.getParameterMap());
        merged.putAll(this.customParams);
        return Collections.unmodifiableMap(merged);
    }
}
