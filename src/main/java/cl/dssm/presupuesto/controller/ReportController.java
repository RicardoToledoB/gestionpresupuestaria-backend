package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/executive.xlsx")
    public ResponseEntity<byte[]> executiveExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ejecutivo_presupuestario.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reportService.executiveExcel());
    }

    @GetMapping("/executive.pdf")
    public ResponseEntity<byte[]> executivePdf() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ejecutivo_presupuestario.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportService.executivePdf());
    }
}
