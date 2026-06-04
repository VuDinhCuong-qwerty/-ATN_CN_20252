package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthUserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserAddressRepository extends JpaRepository<AuthUserAddress, Long> {
}
