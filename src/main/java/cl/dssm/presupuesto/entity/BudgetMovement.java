package cl.dssm.presupuesto.entity;

import cl.dssm.presupuesto.enums.MovementType;
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
@Table(name = "budget_movements")
public class BudgetMovement extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private BudgetProgram program;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MovementType type;

    @Column(nullable = false)
    private LocalDate movementDate;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 120)
    private String exemptResolution;

    @Column(length = 1200)
    private String description;
}
