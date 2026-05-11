package cl.dssm.presupuesto.entity;

import cl.dssm.presupuesto.enums.AlertStatus;
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
@Table(name = "cdps", indexes = {
        @Index(name = "idx_cdp_number", columnList = "cdpNumber", unique = true),
        @Index(name = "idx_cdp_program", columnList = "program_id")
})
public class Cdp extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String cdpNumber;

    private LocalDate cdpDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private BudgetProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    private BudgetItem budgetItem;

    @Column(length = 80)
    private String cdpType;

    @Column(length = 200)
    private String tenderOrContract;

    @Column(length = 1500)
    private String description;

    private LocalDate coverageStart;
    private LocalDate coverageEnd;

    @Column(precision = 8, scale = 2)
    private BigDecimal coverageMonths;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cdpAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cdpAdjustment = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal realCdpAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal executedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal executedPercent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal expectedPercent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal deviation = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private AlertStatus alertStatus = AlertStatus.NORMAL;

    @Column(length = 1000)
    private String suggestedAction;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal possibleReleaseAmount = BigDecimal.ZERO;

    @Column(length = 1200)
    private String observation;
}
