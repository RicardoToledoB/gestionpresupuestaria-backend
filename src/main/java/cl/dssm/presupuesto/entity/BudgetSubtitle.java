package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "budget_subtitles", indexes = @Index(name = "idx_budget_subtitle_code", columnList = "code", unique = true))
public class BudgetSubtitle extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 800)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
