package cl.dssm.presupuesto.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public final class CsvExportUtils {
    private CsvExportUtils() {}

    public static String cell(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\r", " ").replace("\n", " ");
        text = text.replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    public static ResponseEntity<String> response(String filename, String content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body("\uFEFF" + content);
    }
}
