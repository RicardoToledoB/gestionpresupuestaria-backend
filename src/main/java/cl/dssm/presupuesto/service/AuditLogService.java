package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.AuditStatsDto;
import cl.dssm.presupuesto.entity.AuditLog;
import cl.dssm.presupuesto.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;

    public void register(String module, String action, String entityName, Long entityId, String businessKey, String previousValue, String newValue, String observation) {
        repository.save(AuditLog.builder()
                .module(module)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .businessKey(businessKey)
                .username(currentUsername())
                .previousValue(limit(previousValue))
                .newValue(limit(newValue))
                .observation(limit(observation))
                .build());
    }

    public AuditStatsDto stats() {
        return new AuditStatsDto(
                repository.count(),
                repository.countActionLike("CRE"),
                repository.countActionLike("EDIT") + repository.countActionLike("UPDATE"),
                repository.countActionLike("DELETE") + repository.countActionLike("ELIM"),
                repository.countActionLike("RESTORE") + repository.countActionLike("RECUP"),
                repository.countActionLike("LOGIN"),
                repository.countModuleLike("IMPORT")
        );
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) return "system";
        return auth.getName();
    }

    private String limit(String value) {
        if (value == null) return null;
        return value.length() <= 3900 ? value : value.substring(0, 3900) + "...";
    }
}
