package com.planify.specification;

import com.planify.dto.TaskDtos.TaskFilter;
import com.planify.entity.Task;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class TaskSpecs {

    private TaskSpecs() {}

    public static Specification<Task> ofProject(Long projectId, TaskFilter filter, List<Long> ftIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));
            predicates.add(cb.isFalse(root.get("deleted")));
            addFilterPredicates(predicates, root, cb, filter, ftIds);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Task> forAssignee(Long assigneeId, List<Long> visibleProjectIds, TaskFilter filter) {
        return (root, query, cb) -> {
            if (visibleProjectIds == null || visibleProjectIds.isEmpty()) {
                return cb.disjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("assigneeId"), assigneeId));
            predicates.add(root.get("projectId").in(visibleProjectIds));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (filter != null) {
                addFilterPredicates(predicates, root, cb, filter, null);
                if (filter.q() != null && !filter.q().isBlank()) {
                    String pattern = "%" + filter.q().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("title")), pattern),
                            cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
                    ));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addFilterPredicates(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<Task> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            TaskFilter f,
            List<Long> ftIds) {
        if (f != null) {
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), f.priority()));
            }
            if (f.assigneeId() != null) {
                predicates.add(cb.equal(root.get("assigneeId"), f.assigneeId()));
            }
            if (f.dueFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), f.dueFrom()));
            }
            if (f.dueTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), f.dueTo()));
            }
        }
        if (ftIds != null) {
            predicates.add(ftIds.isEmpty() ? cb.disjunction() : root.get("id").in(ftIds));
        }
    }
}
