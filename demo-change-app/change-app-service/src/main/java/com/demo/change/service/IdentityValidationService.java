package com.demo.change.service;

public interface IdentityValidationService {

    void validateUserActive(String username);

    void validateUserIsCab(String username);
}
