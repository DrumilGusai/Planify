package com.planify.repository;

import com.planify.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndDeletedFalse(Long id);

    @Query(value = "SELECT id FROM tasks WHERE project_id = :pid AND is_deleted = 0 AND MATCH(title, description) AGAINST(:q IN BOOLEAN MODE)", nativeQuery = true)
    List<Long> searchIds(@Param("pid") Long projectId, @Param("q") String booleanQuery);

    @Query(value = "SELECT id FROM tasks WHERE project_id = :pid AND is_deleted = 0 AND (LOWER(title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(description, '')) LIKE LOWER(CONCAT('%', :q, '%')))", nativeQuery = true)
    List<Long> searchIdsLike(@Param("pid") Long projectId, @Param("q") String query);
}
