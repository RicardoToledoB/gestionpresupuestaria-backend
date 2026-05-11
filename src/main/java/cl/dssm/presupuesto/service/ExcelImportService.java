package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.ImportResultDto;
import cl.dssm.presupuesto.entity.*;
import cl.dssm.presupuesto.enums.AlertStatus;
import cl.dssm.presupuesto.enums.MovementType;
import cl.dssm.presupuesto.enums.PurchaseOrderStatus;
import cl.dssm.presupuesto.repository.*;
import cl.dssm.presupuesto.util.ExcelReaderUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelImportService {
    private final ExcelImportRepository importRepository;
    private final ExcelImportErrorRepository errorRepository;
    private final BudgetProgramRepository programRepository;
    private final ProviderRepository providerRepository;
    private final BudgetItemRepository itemRepository;
    private final BudgetMovementRepository movementRepository;
    private final CdpRepository cdpRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public ImportResultDto importWorkbook(MultipartFile file) {
        ExcelImport excelImport = ExcelImport.builder()
                .filename(file.getOriginalFilename())
                .status("PROCESSED")
                .programsDetected(0)
                .movementsDetected(0)
                .cdpDetected(0)
                .purchaseOrdersDetected(0)
                .errorsDetected(0)
                .build();
        excelImport = importRepository.save(excelImport);

        List<ExcelImportError> errors = new ArrayList<>();
        int programs = 0, movements = 0, cdps = 0, orders = 0;

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet movementsSheet = wb.getSheet("Movimientos_Ppto");
            if (movementsSheet != null) {
                movements = importBudgetMovements(movementsSheet, excelImport, errors);
                programs = (int) programRepository.count();
            }

            Sheet cdpSheet = wb.getSheet("CDP");
            if (cdpSheet != null) {
                cdps = importCdps(cdpSheet, excelImport, errors);
            }

            Sheet ocSheet = wb.getSheet("OC");
            if (ocSheet != null) {
                orders = importPurchaseOrders(ocSheet, excelImport, errors);
                recalculateCdps();
            }
        } catch (Exception ex) {
            errors.add(error(excelImport, "WORKBOOK", 0, "ERROR", "No fue posible importar el archivo: " + ex.getMessage()));
        }

        errorRepository.saveAll(errors);
        excelImport.setProgramsDetected(programs);
        excelImport.setMovementsDetected(movements);
        excelImport.setCdpDetected(cdps);
        excelImport.setPurchaseOrdersDetected(orders);
        excelImport.setErrorsDetected(errors.size());
        excelImport.setStatus(errors.isEmpty() ? "PROCESSED" : "PROCESSED_WITH_WARNINGS");
        importRepository.save(excelImport);

        return new ImportResultDto(excelImport.getId(), excelImport.getStatus(), programs, movements, cdps, orders, errors.size());
    }

    private int importBudgetMovements(Sheet sheet, ExcelImport excelImport, List<ExcelImportError> errors) {
        int count = 0;

        // En esta planilla la fila 3 contiene encabezados y la data real parte en la fila 4.
        // Apache POI usa índices base 0, por eso comenzamos en 3.
        for (int i = 3; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            try {
                String programName = ExcelReaderUtils.text(row, 1);
                if (programName == null) continue;

                LocalDate movementDate = ExcelReaderUtils.date(row, 0);
                if (movementDate == null) {
                    errors.add(error(excelImport, "Movimientos_Ppto", i + 1, "WARN",
                            "Movimiento omitido: la fecha del movimiento está vacía o no tiene formato válido."));
                    continue;
                }

                // Columnas reales de la hoja Movimientos_Ppto:
                // F = Monto aumenta, G = Monto rebaja, H = Impacto neto.
                BigDecimal increase = ExcelReaderUtils.money(row, 5);
                BigDecimal decrease = ExcelReaderUtils.money(row, 6);
                BigDecimal netImpact = ExcelReaderUtils.money(row, 7);
                BigDecimal amount = netImpact.compareTo(BigDecimal.ZERO) != 0 ? netImpact : increase.subtract(decrease);

                BudgetProgram program = programRepository.findByNameIgnoreCase(programName)
                        .orElseGet(() -> programRepository.save(BudgetProgram.builder()
                                .name(programName)
                                .initialBudget(BigDecimal.ZERO)
                                .currentBudget(BigDecimal.ZERO)
                                .active(true)
                                .build()));

                String typeText = ExcelReaderUtils.text(row, 2);
                MovementType type = detectMovementType(typeText, amount);

                movementRepository.save(BudgetMovement.builder()
                        .program(program)
                        .type(type)
                        .movementDate(movementDate)
                        .amount(amount)
                        .exemptResolution(ExcelReaderUtils.text(row, 3))
                        .description(ExcelReaderUtils.text(row, 4))
                        .build());

                program.setCurrentBudget(nullToZero(program.getCurrentBudget()).add(amount));
                if (type == MovementType.PRESUPUESTO_INICIAL) {
                    program.setInitialBudget(amount);
                }
                programRepository.save(program);
                count++;
            } catch (Exception ex) {
                errors.add(error(excelImport, "Movimientos_Ppto", i + 1, "ERROR",
                        "Movimiento omitido por error de lectura/importación: " + ex.getMessage()));
            }
        }
        return count;
    }

    private int importCdps(Sheet sheet, ExcelImport excelImport, List<ExcelImportError> errors) {
        int count = 0;

        // En esta hoja la fila 3 contiene encabezados y la data real parte en la fila 4.
        for (int i = 3; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            try {
                // Se usa ID_CDP como identificador principal, porque la OC asocia contra este campo.
                String cdpNumber = ExcelReaderUtils.text(row, 2);
                String programName = ExcelReaderUtils.text(row, 5);
                if (cdpNumber == null || programName == null) continue;

                BudgetProgram program = programRepository.findByNameIgnoreCase(programName)
                        .orElseGet(() -> programRepository.save(BudgetProgram.builder()
                                .name(programName)
                                .initialBudget(BigDecimal.ZERO)
                                .currentBudget(BigDecimal.ZERO)
                                .active(true)
                                .build()));
                Provider provider = resolveProvider(row, 7, 8);
                BudgetItem item = resolveItem(ExcelReaderUtils.text(row, 10));

                BigDecimal cdpAmount = ExcelReaderUtils.money(row, 15);
                BigDecimal adjustment = ExcelReaderUtils.money(row, 16);
                BigDecimal realAmountFromSheet = ExcelReaderUtils.money(row, 17);
                BigDecimal realAmount = realAmountFromSheet.compareTo(BigDecimal.ZERO) != 0
                        ? realAmountFromSheet
                        : cdpAmount.add(adjustment);

                Cdp cdp = cdpRepository.findByCdpNumber(cdpNumber).orElse(new Cdp());
                cdp.setCdpNumber(cdpNumber);
                cdp.setCdpDate(ExcelReaderUtils.date(row, 3));
                cdp.setProgram(program);
                cdp.setCdpType(ExcelReaderUtils.text(row, 6));
                cdp.setProvider(provider);
                cdp.setTenderOrContract(ExcelReaderUtils.text(row, 9));
                cdp.setBudgetItem(item);
                cdp.setDescription(ExcelReaderUtils.text(row, 11));
                cdp.setCoverageStart(ExcelReaderUtils.date(row, 12));
                cdp.setCoverageEnd(ExcelReaderUtils.date(row, 13));
                cdp.setCoverageMonths(ExcelReaderUtils.money(row, 14));
                cdp.setCdpAmount(cdpAmount);
                cdp.setCdpAdjustment(adjustment);
                cdp.setRealCdpAmount(realAmount);
                cdp.setExecutedAmount(ExcelReaderUtils.money(row, 18));
                cdp.setPendingBalance(ExcelReaderUtils.money(row, 19));
                cdp.setExecutedPercent(ExcelReaderUtils.money(row, 20));
                cdp.setExpectedPercent(ExcelReaderUtils.money(row, 21));
                cdp.setDeviation(ExcelReaderUtils.money(row, 22));
                cdp.setAlertStatus(parseAlertStatus(ExcelReaderUtils.text(row, 23)));
                cdp.setSuggestedAction(ExcelReaderUtils.text(row, 24));
                cdp.setPossibleReleaseAmount(ExcelReaderUtils.money(row, 25));
                cdpRepository.save(cdp);
                count++;
            } catch (Exception ex) {
                errors.add(error(excelImport, "CDP", i + 1, "ERROR",
                        "CDP omitido por error de lectura/importación: " + ex.getMessage()));
            }
        }
        return count;
    }

    private int importPurchaseOrders(Sheet sheet, ExcelImport excelImport, List<ExcelImportError> errors) {
        int count = 0;

        // En esta hoja la fila 3 contiene encabezados y la data real parte en la fila 4.
        for (int i = 3; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            try {
                String orderNumber = ExcelReaderUtils.text(row, 2);
                if (orderNumber == null) continue;

                String programName = ExcelReaderUtils.text(row, 6);
                if (programName == null) {
                    errors.add(error(excelImport, "OC", i + 1, "WARN",
                            "Orden de compra omitida: no tiene programa asociado."));
                    continue;
                }

                BudgetProgram program = programRepository.findByNameIgnoreCase(programName)
                        .orElseGet(() -> programRepository.save(BudgetProgram.builder()
                                .name(programName)
                                .initialBudget(BigDecimal.ZERO)
                                .currentBudget(BigDecimal.ZERO)
                                .active(true)
                                .build()));
                Provider provider = resolveProvider(row, 4, 5);
                BudgetItem item = resolveItem(ExcelReaderUtils.text(row, 8));
                Cdp cdp = cdpRepository.findByCdpNumber(ExcelReaderUtils.text(row, 7)).orElse(null);

                BigDecimal committed = ExcelReaderUtils.money(row, 9);
                BigDecimal adjustment = ExcelReaderUtils.money(row, 10);
                BigDecimal realAmountFromSheet = ExcelReaderUtils.money(row, 11);
                BigDecimal realAmount = realAmountFromSheet.compareTo(BigDecimal.ZERO) != 0
                        ? realAmountFromSheet
                        : committed.add(adjustment);

                PurchaseOrder po = purchaseOrderRepository.findByOrderNumber(orderNumber).orElse(new PurchaseOrder());
                po.setOrderNumber(orderNumber);
                po.setOrderDate(ExcelReaderUtils.date(row, 1));
                po.setSigfeFolio(ExcelReaderUtils.text(row, 3));
                po.setProvider(provider);
                po.setProgram(program);
                po.setCdp(cdp);
                po.setBudgetItem(item);
                po.setCommittedAmount(committed);
                po.setAdjustmentAmount(adjustment);
                po.setRealAmount(realAmount);
                po.setStatus(parsePurchaseOrderStatus(ExcelReaderUtils.text(row, 13)));
                po.setObservation(ExcelReaderUtils.text(row, 14));
                po.setSubtitle(ExcelReaderUtils.text(row, 15));
                purchaseOrderRepository.save(po);
                count++;
            } catch (Exception ex) {
                errors.add(error(excelImport, "OC", i + 1, "ERROR",
                        "Orden de compra omitida por error de lectura/importación: " + ex.getMessage()));
            }
        }
        return count;
    }

    private void recalculateCdps() {
        for (Cdp cdp : cdpRepository.findAll()) {
            BigDecimal executed = purchaseOrderRepository.sumRealAmountByCdpId(cdp.getId());
            BigDecimal pending = cdp.getRealCdpAmount().subtract(executed);
            BigDecimal pct = cdp.getRealCdpAmount().compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : executed.multiply(BigDecimal.valueOf(100)).divide(cdp.getRealCdpAmount(), 4, RoundingMode.HALF_UP);
            cdp.setExecutedAmount(executed);
            cdp.setPendingBalance(pending.max(BigDecimal.ZERO));
            cdp.setExecutedPercent(pct);
            cdp.setAlertStatus(resolveAlert(cdp));
            cdp.setSuggestedAction(resolveAction(cdp.getAlertStatus()));
            cdp.setPossibleReleaseAmount(cdp.getAlertStatus() == AlertStatus.REVISAR_REBAJA_CDP ? cdp.getPendingBalance() : BigDecimal.ZERO);
            cdpRepository.save(cdp);
        }
    }

    private AlertStatus resolveAlert(Cdp cdp) {
        if (cdp.getPendingBalance().compareTo(BigDecimal.ZERO) <= 0) return AlertStatus.CERRADO;
        if (cdp.getExecutedPercent().compareTo(BigDecimal.valueOf(20)) < 0 && cdp.getPendingBalance().compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            return AlertStatus.REVISAR_REBAJA_CDP;
        }
        if (cdp.getExecutedPercent().compareTo(BigDecimal.valueOf(50)) < 0) return AlertStatus.SEGUIMIENTO;
        return AlertStatus.NORMAL;
    }

    private String resolveAction(AlertStatus status) {
        return switch (status) {
            case CERRADO -> "Sin acción requerida";
            case NORMAL -> "Monitorear ejecución regular";
            case SEGUIMIENTO -> "Revisar avance de emisión de OC y compromisos asociados";
            case ALERTA -> "Escalar revisión administrativa y presupuestaria";
            case REVISAR_REBAJA_CDP -> "Analizar saldo pendiente y evaluar rebaja mediante resolución exenta";
        };
    }

    private Provider resolveProvider(Row row, int rutIndex, int nameIndex) {
        String rut = ExcelReaderUtils.text(row, rutIndex);
        String name = ExcelReaderUtils.text(row, nameIndex);
        if (rut == null && name == null) return null;
        String safeRut = rut == null ? "SIN-RUT-" + Math.abs(name.hashCode()) : rut;
        return providerRepository.findByRut(safeRut)
                .orElseGet(() -> providerRepository.save(Provider.builder()
                        .rut(safeRut)
                        .businessName(name == null ? "Proveedor sin nombre" : name)
                        .active(true)
                        .build()));
    }

    private BudgetItem resolveItem(String raw) {
        if (raw == null) return null;
        String code = raw.split(" ")[0].trim();
        return itemRepository.findByCode(code)
                .orElseGet(() -> itemRepository.save(BudgetItem.builder().code(code).name(raw).active(true).build()));
    }


    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private AlertStatus parseAlertStatus(String value) {
        if (value == null) return AlertStatus.NORMAL;
        String normalized = value.trim().toUpperCase()
                .replace(" ", "_")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");
        try {
            return AlertStatus.valueOf(normalized);
        } catch (Exception ex) {
            return AlertStatus.NORMAL;
        }
    }

    private PurchaseOrderStatus parsePurchaseOrderStatus(String value) {
        if (value == null) return PurchaseOrderStatus.EMITIDA;
        String normalized = value.trim().toUpperCase()
                .replace(" ", "_")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");
        try {
            return PurchaseOrderStatus.valueOf(normalized);
        } catch (Exception ex) {
            return PurchaseOrderStatus.EMITIDA;
        }
    }

    private MovementType detectMovementType(String text, BigDecimal amount) {
        if (text == null) return amount.signum() < 0 ? MovementType.REBAJA : MovementType.AUMENTO;
        String t = text.toUpperCase();
        if (t.contains("INICIAL")) return MovementType.PRESUPUESTO_INICIAL;
        if (t.contains("REBAJA")) return MovementType.REBAJA;
        if (t.contains("AJUSTE")) return MovementType.AJUSTE_INTERNO;
        if (t.contains("DISTRIB")) return MovementType.DISTRIBUCION;
        return amount.signum() < 0 ? MovementType.REBAJA : MovementType.AUMENTO;
    }

    private ExcelImportError error(ExcelImport excelImport, String sheet, int row, String severity, String message) {
        return ExcelImportError.builder().excelImport(excelImport).sheetName(sheet).rowNumber(row).severity(severity).message(message).build();
    }
}
