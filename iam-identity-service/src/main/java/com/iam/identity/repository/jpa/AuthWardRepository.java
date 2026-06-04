package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthWard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthWardRepository extends JpaRepository<AuthWard, Long> {

    @Query(value = "SELECT * FROM AUTH_WARD WHERE PROVINCE_CODE = :provinceCode ORDER BY NAME", nativeQuery = true)
    List<AuthWard> findByProvinceCode(@Param("provinceCode") Long provinceCode);

    @Query(value = "SELECT * FROM AUTH_WARD WHERE CODENAME = :codename", nativeQuery = true)
    Optional<AuthWard> findByCodename(@Param("codename") String codename);

    @Query(value = "SELECT * FROM AUTH_WARD WHERE PROVINCE_CODE = :provinceCode AND STATUS = :status ORDER BY NAME", nativeQuery = true)
    List<AuthWard> findByProvinceCodeAndStatus(@Param("provinceCode") Long provinceCode, @Param("status") String status);

    @Query(value = "SELECT * FROM AUTH_WARD WHERE PROVINCE_CODE = :provinceCode AND STATUS = :status AND UPPER(NAME) LIKE UPPER('%' || :name || '%') ORDER BY NAME", nativeQuery = true)
    List<AuthWard> findByProvinceCodeAndStatusAndNameContaining(@Param("provinceCode") Long provinceCode, @Param("status") String status, @Param("name") String name);
}
