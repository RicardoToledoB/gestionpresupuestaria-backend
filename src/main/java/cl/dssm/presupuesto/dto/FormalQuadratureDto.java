package cl.dssm.presupuesto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FormalQuadratureDto(
        Long importId,
        String filename,
        LocalDateTime importedAt,
        String status,
        List<Row> rows,
        String recommendation
) {
    public record Row(
            String indicator,
            BigDecimal excelValue,
            BigDecimal systemValue,
            BigDecimal difference,
            String status
    ) {}
}
