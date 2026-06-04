package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthUserResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthUserResourceRepository extends JpaRepository<AuthUserResource, Long> {

    @Query(value = """
            SELECT * FROM AUTH_USER_RESOURCE
            WHERE USER_ID = :userId
              AND APP_ID  = :appId
              AND STATUS  = 'ACTIVE'
              AND (EXPIRED_AT IS NULL OR EXPIRED_AT > CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    List<AuthUserResource> findActiveByUserIdAndAppId(@Param("userId") Long userId,
                                                      @Param("appId") Long appId);
}
