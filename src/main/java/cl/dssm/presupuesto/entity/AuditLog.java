package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_module", columnList = "module"),
        @Index(name = "idx_audit_entity", columnList = "entityName,entityId")
})
public class AuditLog extends BaseEntity {
    @Column(nullable = false, length = 80)
    private String module;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 120)
    private String entityName;

    private Long entityId;

    @Column(length = 120)
    private String businessKey;

    @Column(length = 120)
    private String username;

    @Column(length = 4000)
    private String previousValue;

    @Column(length = 4000)
    private String newValue;

    @Column(length = 1200)
    private String observation;
}
