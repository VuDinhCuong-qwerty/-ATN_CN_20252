package com.iam.auth.engine.authenticator;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Registry map method → authenticator impl
@Component
public class AuthenticatorRegistry {

    private final Map<String, Authenticator> registry;

    public AuthenticatorRegistry(List<Authenticator> authenticators) {
        registry = authenticators.stream()
                .collect(Collectors.toMap(Authenticator::getMethod, a -> a));
    }

    public Authenticator get(String method) {
        return registry.get(method);
    }
}
