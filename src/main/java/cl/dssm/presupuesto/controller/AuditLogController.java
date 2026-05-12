package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.AuditLogResponseDto;
import cl.dssm.presupuesto.dto.AuditStatsDto;
import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.repository.AuditLogRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogRepository repository;
    private final cl.dssm.presupuesto.service.AuditLogService auditLogService;

    @GetMapping("/stats")
    public AuditStatsDto stats() {
        return auditLogService.stats();
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponseDto<AuditLogResponseDto> findAll(@RequestParam(defaultValue = "") String search, @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageResponseDto.from(repository.search(search.trim(), pageable).map(AuditLogResponseDto::fromEntity));
    }

    @GetMapping("/export")
    @Transactional(readOnly = true)
    public ResponseEntity<String> export(@RequestParam(defaultValue = "") String search) {
        String header = "ID;Fecha;Modulo;Accion;Entidad;IDEntidad;Clave;Usuario;Observacion;ValorAnterior;ValorNuevo\n";
        String rows = repository.exportRows(search.trim()).stream()
                .map(a -> String.join(";", CsvExportUtils.cell(a.getId()), CsvExportUtils.cell(a.getCreatedAt()), CsvExportUtils.cell(a.getModule()), CsvExportUtils.cell(a.getAction()), CsvExportUtils.cell(a.getEntityName()), CsvExportUtils.cell(a.getEntityId()), CsvExportUtils.cell(a.getBusinessKey()), CsvExportUtils.cell(a.getUsername()), CsvExportUtils.cell(a.getObservation()), CsvExportUtils.cell(a.getPreviousValue()), CsvExportUtils.cell(a.getNewValue())))
                .collect(Collectors.joining("\n"));
        return CsvExportUtils.response("auditoria_filtrada.csv", header + rows);
    }
}
