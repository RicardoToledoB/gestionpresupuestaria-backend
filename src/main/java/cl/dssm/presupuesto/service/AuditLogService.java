package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.entity.AuditLog;
import cl.dssm.presupuesto.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
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
                .username("system")
                .previousValue(limit(previousValue))
                .newValue(limit(newValue))
                .observation(limit(observation))
                .build());
    }

    private String limit(String value) {
        if (value == null) return null;
        return value.length() <= 3900 ? value : value.substring(0, 3900) + "...";
    }
}
