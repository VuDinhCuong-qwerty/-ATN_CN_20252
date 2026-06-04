package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthSigningKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthSigningKeyRepository extends JpaRepository<AuthSigningKey, Long> {

    @Query(value = "SELECT * FROM AUTH_SIGNING_KEY WHERE STATUS = :status ORDER BY VALID_FROM DESC FETCH FIRST 1 ROWS ONLY", nativeQuery = true)
    Optional<AuthSigningKey> findFirstByStatus(@Param("status") String status);

    @Query(value = "SELECT * FROM AUTH_SIGNING_KEY WHERE STATUS != :status", nativeQuery = true)
    List<AuthSigningKey> findByStatusNot(@Param("status") String status);

    @Query(value = "SELECT * FROM AUTH_SIGNING_KEY WHERE STATUS = :status AND VALID_UNTIL <= :dateTime", nativeQuery = true)
    List<AuthSigningKey> findByStatusAndValidUntilBefore(@Param("status") String status, @Param("dateTime") LocalDateTime dateTime);

    @Query(value = "SELECT * FROM AUTH_SIGNING_KEY WHERE KID = :kid", nativeQuery = true)
    Optional<AuthSigningKey> findByKid(@Param("kid") String kid);
}
