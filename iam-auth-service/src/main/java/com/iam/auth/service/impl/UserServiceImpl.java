package com.iam.auth.service.impl;

import com.iam.auth.domain.AuthUser;
import com.iam.auth.service.BaseService;
import com.iam.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends BaseService implements UserService {

    @Override
    public AuthUser getUserByUsername(String username) {
        return null;
    }
}
