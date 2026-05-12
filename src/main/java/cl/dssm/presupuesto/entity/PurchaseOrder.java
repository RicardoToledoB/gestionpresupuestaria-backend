package cl.dssm.presupuesto.entity;

import cl.dssm.presupuesto.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_oc_number", columnList = "orderNumber", unique = true),
        @Index(name = "idx_oc_cdp", columnList = "cdp_id")
})
public class PurchaseOrder extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String orderNumber;

    private LocalDate orderDate;

    @Column(length = 80)
    private String sigfeFolio;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private BudgetProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    private BudgetItem budgetItem;

    @ManyToOne(fetch = FetchType.LAZY)
    private Cdp cdp;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal committedAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal realAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.EMITIDA;

    @Column(length = 1200)
    private String observation;

    @Column(length = 50)
    private String subtitle;
}
