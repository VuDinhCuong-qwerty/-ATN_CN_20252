package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthClientGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthClientGroupRepository extends JpaRepository<AuthClientGroup, Long> {

    @Query(value = "SELECT * FROM AUTH_CLIENT_GROUP WHERE (:id IS NULL OR ID = :id) AND (:status IS NULL OR STATUS = :status)", nativeQuery = true)
    List<AuthClientGroup> findWithFilters(@Param("id") Long id, @Param("status") Integer status);

    @Query(value = "SELECT COUNT(1) FROM AUTH_CLIENT_GROUP WHERE NAME = :name", nativeQuery = true)
    int countByName(@Param("name") String name);

    @Query(value = "SELECT COUNT(1) FROM AUTH_CLIENT_GROUP WHERE NAME = :name AND ID <> :id", nativeQuery = true)
    int countByNameAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Query(value = "SELECT COUNT(1) FROM AUTH_CLIENT_GROUP WHERE ID = :id", nativeQuery = true)
    int countById(@Param("id") Long id);
}
