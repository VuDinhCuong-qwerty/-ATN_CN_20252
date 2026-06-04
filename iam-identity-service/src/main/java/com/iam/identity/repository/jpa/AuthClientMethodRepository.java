package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthClientMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthClientMethodRepository extends JpaRepository<AuthClientMethod, Long> {

    List<AuthClientMethod> findByAppId(Long appId);

    List<AuthClientMethod> findByAppIdAndStatus(Long appId, String status);

    boolean existsByAppIdAndMethodId(Long appId, Long methodId);
}
