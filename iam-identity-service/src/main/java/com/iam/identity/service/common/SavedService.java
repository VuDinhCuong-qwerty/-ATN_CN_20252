package com.iam.identity.service.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.iam.identity.domain.AuthUser;
import com.iam.identity.domain.AuthUserAddress;
import com.iam.identity.domain.AuthUserProfile;
import com.iam.identity.domain.AuthUserRole;
import com.iam.identity.dto.request.CreateUserRequest;
import com.iam.identity.dto.response.CreateUserResponse;
import com.iam.identity.repository.jpa.AuthUserAddressRepository;
import com.iam.identity.repository.jpa.AuthUserProfileRepository;
import com.iam.identity.repository.jpa.AuthUserRepository;
import com.iam.identity.repository.jpa.AuthUserRoleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedService {

    private final AuthUserRepository userRepository;
    private final AuthUserProfileRepository userProfileRepository;
    private final AuthUserAddressRepository userAddressRepository;
    private final AuthUserRoleRepository userRoleRepository;


    @Transactional
    public CreateUserResponse createUser(AuthUser user, AuthUserProfile userProfile,
            List<AuthUserAddress> userAddresses, List<AuthUserRole> authUserRoles, CreateUserRequest input) {
        CreateUserResponse output = new CreateUserResponse();
        AuthUser savedUser = userRepository.save(user);
        userProfile.setUserId(savedUser.getId());
        AuthUserProfile savedUserProfile = userProfileRepository.save(userProfile);
        for (AuthUserAddress item : userAddresses) {
            item.setUserId(savedUser.getId());
        }
        userAddressRepository.saveAll(userAddresses);
        for (AuthUserRole item : authUserRoles) {
            item.setUserId(savedUser.getId());
        }
        userRoleRepository.saveAll(authUserRoles);
        output.setUserId(savedUser.getId());
        output.setUsername(savedUser.getUsername());
        output.setFullName(savedUserProfile.getFullName());
        output.setEmail(savedUser.getEmail());
        output.setStatus("ACTIVE");
        output.setCreatedAt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        List<String> roleStrings = input.getRoles().stream().map(item -> item.getCode()).collect(Collectors.toList());
        output.setRoles(roleStrings);
        return output;
    }

}
