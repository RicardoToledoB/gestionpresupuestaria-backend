package cl.dssm.presupuesto.util;

import org.apache.poi.ss.usermodel.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

public final class ExcelReaderUtils {
    private ExcelReaderUtils() {}

    public static String text(Row row, int index) {
        if (row == null || row.getCell(index) == null) return null;
        Cell cell = row.getCell(index);
        return switch (cell.getCellType()) {
            case STRING -> clean(cell.getStringCellValue());
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : clean(BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> formulaText(cell);
            default -> null;
        };
    }

    public static BigDecimal money(Row row, int index) {
        if (row == null || row.getCell(index) == null) return BigDecimal.ZERO;
        Cell cell = row.getCell(index);
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> parseMoney(cell.getStringCellValue());
                case FORMULA -> BigDecimal.valueOf(cell.getNumericCellValue());
                default -> BigDecimal.ZERO;
            };
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    public static LocalDate date(Row row, int index) {
        if (row == null || row.getCell(index) == null) return null;
        Cell cell = row.getCell(index);
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().isBlank()) {
                return LocalDate.parse(cell.getStringCellValue().trim());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String formulaText(Cell cell) {
        try {
            return clean(cell.getStringCellValue());
        } catch (Exception ex) {
            try { return clean(BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()); }
            catch (Exception ignored) { return null; }
        }
    }

    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private static BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        String normalized = value.replace("$", "").replace(".", "").replace(",", ".").trim();
        return new BigDecimal(normalized);
    }
}
