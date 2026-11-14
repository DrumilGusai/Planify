package com.planify.audit;

import com.planify.dto.AuditDtos.AuditFilter;
import com.planify.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AuditSpecs {

    private AuditSpecs() {}

    public static Specification<AuditLog> forWorkspace(Long workspaceId, AuditFilter filter) {
        return build(workspaceId, null, filter);
    }

    public static Specification<AuditLog> forProject(Long projectId, AuditFilter filter) {
        return build(null, projectId, filter);
    }

    private static Specification<AuditLog> build(Long workspaceId, Long projectId, AuditFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (workspaceId != null) {
                predicates.add(cb.equal(root.get("workspaceId"), workspaceId));
            }
            if (projectId != null) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (f != null) {
                if (f.entityType() != null && !f.entityType().isBlank()) {
                    predicates.add(cb.equal(root.get("entityType"), f.entityType().trim().toUpperCase()));
                }
                if (f.actorId() != null) {
                    predicates.add(cb.equal(root.get("actorId"), f.actorId()));
                }
                if (f.from() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from()));
                }
                if (f.to() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), f.to()));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
