package com.iam.auth.service;

import com.iam.auth.domain.AuthUser;

public interface UserService {
    AuthUser getUserByUsername(String username);
}
