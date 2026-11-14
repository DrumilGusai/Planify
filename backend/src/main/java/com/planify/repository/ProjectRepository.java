package com.planify.repository;

import com.planify.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    boolean existsByWorkspaceIdAndName(Long workspaceId, String name);

    @Query("select p from Project p join ProjectMember m on m.projectId = p.id where p.workspaceId = :wsId and m.userId = :uid order by p.createdAt desc")
    List<Project> findVisibleForMember(@Param("wsId") Long workspaceId, @Param("uid") Long userId);

    @Query("""
        select distinct p.id from Project p
        left join ProjectMember pm on pm.projectId = p.id and pm.userId = :uid
        left join WorkspaceMember wm on wm.workspaceId = p.workspaceId and wm.userId = :uid
        where pm.id is not null or wm.role in (com.planify.entity.WorkspaceRole.OWNER, com.planify.entity.WorkspaceRole.ADMIN)
        """)
    List<Long> findAccessibleProjectIds(@Param("uid") Long userId);
}
