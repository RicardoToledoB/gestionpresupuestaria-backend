package cl.dssm.presupuesto.audit;

import cl.dssm.presupuesto.entity.AuditLog;
import cl.dssm.presupuesto.entity.BaseEntity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

public class AuditEntityListener {

    @PostLoad
    public void postLoad(Object entity) {
        if (entity instanceof BaseEntity base && !(entity instanceof AuditLog)) {
            base.setAuditPreviousSnapshot(AuditSerializationUtils.snapshot(entity));
        }
    }

    @PostPersist
    public void postPersist(Object entity) {
        if (!(entity instanceof BaseEntity base) || entity instanceof AuditLog) return;
        AuditJdbcWriter writer = AuditJdbcWriter.getInstance();
        if (writer == null) return;
        String current = AuditSerializationUtils.snapshot(entity);
        writer.write(
                AuditSerializationUtils.moduleName(entity),
                "CREATE",
                entity.getClass().getSimpleName(),
                base.getId(),
                AuditSerializationUtils.businessKey(entity),
                null,
                current,
                "Creación de registro en " + AuditSerializationUtils.moduleName(entity)
        );
        base.setAuditPreviousSnapshot(current);
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        if (!(entity instanceof BaseEntity base) || entity instanceof AuditLog) return;
        AuditJdbcWriter writer = AuditJdbcWriter.getInstance();
        if (writer == null) return;
        String previous = base.getAuditPreviousSnapshot();
        if (!AuditSerializationUtils.changed(previous, entity)) return;
        String action = AuditSerializationUtils.isSoftDelete(previous, entity) ? "SOFT_DELETE" : AuditSerializationUtils.isRestore(previous, entity) ? "RESTORE" : "UPDATE";
        String current = AuditSerializationUtils.snapshot(entity);
        writer.write(
                AuditSerializationUtils.moduleName(entity),
                action,
                entity.getClass().getSimpleName(),
                base.getId(),
                AuditSerializationUtils.businessKey(entity),
                previous,
                current,
                observation(action, AuditSerializationUtils.moduleName(entity))
        );
        base.setAuditPreviousSnapshot(current);
    }

    @PostRemove
    public void postRemove(Object entity) {
        if (!(entity instanceof BaseEntity base) || entity instanceof AuditLog) return;
        AuditJdbcWriter writer = AuditJdbcWriter.getInstance();
        if (writer == null) return;
        writer.write(
                AuditSerializationUtils.moduleName(entity),
                "HARD_DELETE",
                entity.getClass().getSimpleName(),
                base.getId(),
                AuditSerializationUtils.businessKey(entity),
                base.getAuditPreviousSnapshot(),
                null,
                "Eliminación física de registro en " + AuditSerializationUtils.moduleName(entity)
        );
    }

    private String observation(String action, String module) {
        return switch (action) {
            case "SOFT_DELETE" -> "Eliminación lógica de registro en " + module;
            case "RESTORE" -> "Recuperación de registro eliminado en " + module;
            default -> "Actualización de registro en " + module;
        };
    }
}
