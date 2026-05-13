package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_order_states", indexes = @Index(name = "idx_po_state_name", columnList = "name", unique = true))
public class PurchaseOrderState extends BaseEntity {
    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 50)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
