package com.planify.repository;

import com.planify.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByProjectId(Long projectId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);

    @Modifying
    @Query("delete from ProjectMember pm where pm.userId = :uid and pm.projectId in (select p.id from Project p where p.workspaceId = :wsId)")
    void deleteByWorkspaceIdAndUserId(@Param("wsId") Long workspaceId, @Param("uid") Long userId);
}
