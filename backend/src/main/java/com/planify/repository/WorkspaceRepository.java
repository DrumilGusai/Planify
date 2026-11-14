package com.planify.repository;

import com.planify.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Query("select w from Workspace w join WorkspaceMember m on m.workspaceId = w.id where m.userId = :uid order by w.createdAt desc")
    List<Workspace> findByMemberUserId(@Param("uid") Long userId);
}
