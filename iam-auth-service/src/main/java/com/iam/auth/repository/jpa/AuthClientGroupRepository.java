package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthClientGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthClientGroupRepository extends JpaRepository<AuthClientGroup, Long> {

    @Query(value = "SELECT * FROM AUTH_CLIENT_GROUP a WHERE a.ID = :id", nativeQuery = true)
    List<AuthClientGroup> getGroupById(@Param("id") Long id);
}
