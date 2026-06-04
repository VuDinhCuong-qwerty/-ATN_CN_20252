package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthFlowRepository extends JpaRepository<AuthFlow, Long> {

    List<AuthFlow> findByAppId(Long appId);

    Optional<AuthFlow> findByAppIdAndAlias(Long appId, String alias);

    List<AuthFlow> findByAppIdAndStatus(Long appId, String status);
}
