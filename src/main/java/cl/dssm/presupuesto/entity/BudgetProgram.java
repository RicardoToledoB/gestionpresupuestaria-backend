package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "budget_programs", indexes = {
        @Index(name = "idx_budget_program_name", columnList = "name", unique = true)
})
public class BudgetProgram extends BaseEntity {
    @NotBlank
    @Column(nullable = false, unique = true, length = 180)
    private String name;

    @Column(length = 50)
    private String code;

    @Column(length = 50)
    private String subtitle;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal initialBudget = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal currentBudget = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
