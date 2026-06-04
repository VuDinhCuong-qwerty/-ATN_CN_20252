package com.demo.change.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.demo.change.entity.TeamMember;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query(value = "SELECT * FROM CHG_TEAM_MEMBER WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1 ORDER BY CREATED_AT ASC", nativeQuery = true)
    List<TeamMember> findActiveByChangeRequestId(@Param("changeRequestId") Long changeRequestId);
}
