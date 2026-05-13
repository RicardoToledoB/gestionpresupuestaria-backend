package cl.dssm.presupuesto.dto;

import cl.dssm.presupuesto.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponseDto(
        Long id,
        LocalDateTime createdAt,
        String module,
        String action,
        String entityName,
        Long entityId,
        String businessKey,
        String username,
        String previousValue,
        String newValue,
        String observation,
        String ipAddress,
        String userAgent,
        String httpMethod,
        String requestPath
) {
    public static AuditLogResponseDto fromEntity(AuditLog a) {
        return new AuditLogResponseDto(
                a.getId(),
                a.getCreatedAt(),
                a.getModule(),
                a.getAction(),
                a.getEntityName(),
                a.getEntityId(),
                a.getBusinessKey(),
                a.getUsername(),
                a.getPreviousValue(),
                a.getNewValue(),
                a.getObservation(),
                a.getIpAddress(),
                a.getUserAgent(),
                a.getHttpMethod(),
                a.getRequestPath()
        );
    }
}
