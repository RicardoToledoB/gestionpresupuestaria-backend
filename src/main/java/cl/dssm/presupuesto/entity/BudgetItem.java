package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "budget_items")
public class BudgetItem extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 50)
    private String subtitle;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
