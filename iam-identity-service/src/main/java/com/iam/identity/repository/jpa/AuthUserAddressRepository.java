package com.iam.identity.repository.jpa;

import java.util.List;

import com.iam.identity.domain.AuthUserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserAddressRepository extends JpaRepository<AuthUserAddress, Long> {

    @Query(value = "SELECT * FROM AUTH_USER_ADDRESS a WHERE a.USER_ID = :userId", nativeQuery = true)
    List<AuthUserAddress> findByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM AUTH_USER_ADDRESS a WHERE a.USER_ID = :userId AND a.TYPE = :type", nativeQuery = true)
    List<AuthUserAddress> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query(value = """
            SELECT
                a.TYPE          AS type,
                a.PROVINCE_CODE AS province_code,
                prov.NAME       AS province_name,
                a.WARD_CODE     AS ward_code,
                w.NAME          AS ward_name,
                a.DETAIL        AS detail,
                a.UPDATED_AT    AS updated_at
            FROM AUTH_USER_ADDRESS a
            LEFT JOIN AUTH_PROVINCE prov ON prov.CODE = a.PROVINCE_CODE
            LEFT JOIN AUTH_WARD     w    ON w.CODE    = a.WARD_CODE
            WHERE a.USER_ID = :userId
            ORDER BY a.TYPE
            """, nativeQuery = true)
    List<Object[]> findByUserIdWithNames(@Param("userId") Long userId);
}
