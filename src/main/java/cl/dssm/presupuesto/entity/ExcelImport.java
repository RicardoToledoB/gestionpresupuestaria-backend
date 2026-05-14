package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "excel_imports")
public class ExcelImport extends BaseEntity {
    @Column(nullable = false, length = 250)
    private String filename;
    private Integer programsDetected;
    private Integer cdpDetected;
    private Integer purchaseOrdersDetected;
    private Integer movementsDetected;
    private Integer errorsDetected;
    @Column(length = 40)
    private String status;
}
