package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "excel_import_errors")
public class ExcelImportError extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ExcelImport excelImport;
    @Column(length = 120)
    private String sheetName;
    private Integer rowNumber;
    @Column(length = 1500)
    private String message;
    @Column(length = 80)
    private String severity;
}
