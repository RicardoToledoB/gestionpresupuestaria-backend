package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.DashboardSummaryDto;
import cl.dssm.presupuesto.dto.FormalQuadratureDto;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final DashboardService dashboardService;
    private final FormalQuadratureService quadratureService;
    private final AuditLogService auditLogService;

    public byte[] executiveExcel() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Resumen Ejecutivo");
            int rowIndex = 0;
            Row title = sheet.createRow(rowIndex++);
            title.createCell(0).setCellValue("Plataforma de Gestión Presupuestaria DSSM - Reporte Ejecutivo");
            sheet.createRow(rowIndex++).createCell(0).setCellValue("Generado: " + LocalDateTime.now());
            rowIndex++;
            DashboardSummaryDto s = dashboardService.summary();
            rowIndex = writeMetric(sheet, rowIndex, "Presupuesto vigente", s.totalCurrentBudget());
            rowIndex = writeMetric(sheet, rowIndex, "CDP emitidos", s.totalCdpIssued());
            rowIndex = writeMetric(sheet, rowIndex, "Ejecutado por OC", s.totalExecutedByOc());
            rowIndex = writeMetric(sheet, rowIndex, "Saldo disponible", s.totalAvailableBalance());
            rowIndex = writeMetric(sheet, rowIndex, "Saldo pendiente CDP", s.totalPendingCdpBalance());
            rowIndex = writeMetric(sheet, rowIndex, "Posible liberación", s.totalPossibleRelease());
            rowIndex += 2;
            sheet.createRow(rowIndex++).createCell(0).setCellValue("Cuadratura formal");
            Row header = sheet.createRow(rowIndex++);
            header.createCell(0).setCellValue("Indicador");
            header.createCell(1).setCellValue("Excel");
            header.createCell(2).setCellValue("Sistema");
            header.createCell(3).setCellValue("Diferencia");
            header.createCell(4).setCellValue("Estado");
            for (FormalQuadratureDto.Row q : quadratureService.quadrature().rows()) {
                Row r = sheet.createRow(rowIndex++);
                r.createCell(0).setCellValue(q.indicator());
                r.createCell(1).setCellValue(q.excelValue().doubleValue());
                r.createCell(2).setCellValue(q.systemValue().doubleValue());
                r.createCell(3).setCellValue(q.difference().doubleValue());
                r.createCell(4).setCellValue(q.status());
            }
            for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            auditLogService.register("REPORTES", "EXPORT_EXCEL", "ReporteEjecutivo", null, "reporte_ejecutivo.xlsx", null, null, "Exportación de reporte ejecutivo Excel");
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar reporte Excel: " + ex.getMessage(), ex);
        }
    }

    public byte[] executivePdf() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font nFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            doc.add(new Paragraph("Plataforma de Gestión Presupuestaria DSSM - Reporte Ejecutivo", titleFont));
            doc.add(new Paragraph("Generado: " + LocalDateTime.now(), nFont));
            doc.add(Chunk.NEWLINE);
            DashboardSummaryDto s = dashboardService.summary();
            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(100);
            addPdfMetric(t, "Presupuesto vigente", s.totalCurrentBudget(), hFont, nFont);
            addPdfMetric(t, "CDP emitidos", s.totalCdpIssued(), hFont, nFont);
            addPdfMetric(t, "Ejecutado por OC", s.totalExecutedByOc(), hFont, nFont);
            addPdfMetric(t, "Saldo disponible", s.totalAvailableBalance(), hFont, nFont);
            addPdfMetric(t, "Saldo pendiente CDP", s.totalPendingCdpBalance(), hFont, nFont);
            addPdfMetric(t, "Posible liberación", s.totalPossibleRelease(), hFont, nFont);
            doc.add(t);
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Cuadratura formal Excel vs Sistema", hFont));
            PdfPTable q = new PdfPTable(5);
            q.setWidthPercentage(100);
            for (String head : new String[]{"Indicador", "Excel", "Sistema", "Diferencia", "Estado"}) q.addCell(new PdfPCell(new Phrase(head, hFont)));
            for (FormalQuadratureDto.Row row : quadratureService.quadrature().rows()) {
                q.addCell(new PdfPCell(new Phrase(row.indicator(), nFont)));
                q.addCell(new PdfPCell(new Phrase(format(row.excelValue()), nFont)));
                q.addCell(new PdfPCell(new Phrase(format(row.systemValue()), nFont)));
                q.addCell(new PdfPCell(new Phrase(format(row.difference()), nFont)));
                q.addCell(new PdfPCell(new Phrase(row.status(), nFont)));
            }
            doc.add(q);
            doc.close();
            auditLogService.register("REPORTES", "EXPORT_PDF", "ReporteEjecutivo", null, "reporte_ejecutivo.pdf", null, null, "Exportación de reporte ejecutivo PDF");
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar reporte PDF: " + ex.getMessage(), ex);
        }
    }

    private int writeMetric(Sheet sheet, int rowIndex, String label, BigDecimal value) {
        Row r = sheet.createRow(rowIndex++);
        r.createCell(0).setCellValue(label);
        r.createCell(1).setCellValue(value == null ? 0 : value.doubleValue());
        return rowIndex;
    }

    private void addPdfMetric(PdfPTable t, String label, BigDecimal value, Font hFont, Font nFont) {
        t.addCell(new PdfPCell(new Phrase(label, hFont)));
        t.addCell(new PdfPCell(new Phrase(format(value), nFont)));
    }

    private String format(BigDecimal value) {
        return "$" + String.format("%,.0f", value == null ? BigDecimal.ZERO : value).replace(',', '.');
    }
}
