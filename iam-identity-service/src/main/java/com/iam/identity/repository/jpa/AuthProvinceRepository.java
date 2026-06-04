package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthProvince;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthProvinceRepository extends JpaRepository<AuthProvince, Long> {

    @Query(value = "SELECT * FROM AUTH_PROVINCE WHERE CODENAME = :codename", nativeQuery = true)
    Optional<AuthProvince> findByCodename(@Param("codename") String codename);

    @Query(value = "SELECT * FROM AUTH_PROVINCE WHERE STATUS = :status ORDER BY NAME", nativeQuery = true)
    List<AuthProvince> findByStatus(@Param("status") String status);

    @Query(value = "SELECT * FROM AUTH_PROVINCE WHERE STATUS = :status AND UPPER(NAME) LIKE UPPER('%' || :name || '%') ORDER BY NAME", nativeQuery = true)
    List<AuthProvince> findByStatusAndNameContaining(@Param("status") String status, @Param("name") String name);
}
