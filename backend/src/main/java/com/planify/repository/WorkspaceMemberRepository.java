package com.planify.repository;

import com.planify.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    List<WorkspaceMember> findByUserId(Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    void deleteByWorkspaceIdAndUserId(Long workspaceId, Long userId);
}
