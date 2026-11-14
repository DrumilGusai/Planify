package com.planify.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planify.dto.AuditContext;
import com.planify.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditWriter writer;
    private final AuditSnapshotProvider snapshotProvider;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        AuditSnapshotProvider.Snapshot before = snapshotProvider.captureBefore(
                audited.entity(), audited.action(), pjp.getArgs());

        Object result = pjp.proceed();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
                return result;
            }

            Long workspaceId = before != null ? before.workspaceId() : null;
            Long projectId = before != null ? before.projectId() : null;
            Long entityId = before != null ? before.entityId() : null;

            if (result instanceof AuditContext ctx) {
                if (ctx.workspaceId() != null) workspaceId = ctx.workspaceId();
                if (ctx.projectId() != null) projectId = ctx.projectId();
                if (ctx.entityId() != null) entityId = ctx.entityId();
            }

            String oldJson = before != null && before.state() != null ? toJson(before.state()) : null;
            String newJson = result != null ? toJson(result) : null;
            String ip = extractIp();

            writer.write(new AuditEvent(
                    principal.userId(),
                    workspaceId,
                    projectId,
                    audited.entity(),
                    entityId,
                    audited.action(),
                    oldJson,
                    newJson,
                    ip,
                    Instant.now()
            ));
        } catch (Exception e) {
            log.warn("Audit aspect skipped due to error: {}", e.getMessage());
        }
        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
