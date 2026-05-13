package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.entity.Cdp;
import cl.dssm.presupuesto.enums.AlertStatus;
import cl.dssm.presupuesto.repository.CdpRepository;
import cl.dssm.presupuesto.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CdpRecalculationService {
    private final CdpRepository cdpRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional
    public Cdp recalculate(Long cdpId) {
        Cdp cdp = cdpRepository.findById(cdpId).orElseThrow();
        return recalculate(cdp);
    }

    @Transactional
    public void recalculateAll() {
        cdpRepository.findAll().forEach(this::recalculate);
    }

    public Cdp recalculate(Cdp cdp) {
        BigDecimal real = nullToZero(cdp.getRealCdpAmount());
        BigDecimal executed = cdp.getId() == null ? BigDecimal.ZERO : purchaseOrderRepository.sumRealAmountByCdpId(cdp.getId());
        BigDecimal pending = real.subtract(executed).max(BigDecimal.ZERO);
        BigDecimal pct = real.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : executed.multiply(BigDecimal.valueOf(100)).divide(real, 4, RoundingMode.HALF_UP);
        cdp.setExecutedAmount(executed);
        cdp.setPendingBalance(pending);
        cdp.setExecutedPercent(pct);
        cdp.setDeviation(pct.subtract(nullToZero(cdp.getExpectedPercent())));
        cdp.setAlertStatus(resolveAlert(cdp));
        cdp.setSuggestedAction(resolveAction(cdp.getAlertStatus()));
        cdp.setPossibleReleaseAmount(cdp.getAlertStatus() == AlertStatus.REVISAR_REBAJA_CDP ? pending : BigDecimal.ZERO);
        return cdpRepository.save(cdp);
    }

    private AlertStatus resolveAlert(Cdp cdp) {
        if (cdp.getDeletedAt() != null) return AlertStatus.CERRADO;
        if (nullToZero(cdp.getPendingBalance()).compareTo(BigDecimal.ZERO) <= 0) return AlertStatus.CERRADO;
        if (nullToZero(cdp.getExecutedPercent()).compareTo(BigDecimal.valueOf(20)) < 0 && nullToZero(cdp.getPendingBalance()).compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            return AlertStatus.REVISAR_REBAJA_CDP;
        }
        if (nullToZero(cdp.getExecutedPercent()).compareTo(BigDecimal.valueOf(50)) < 0) return AlertStatus.SEGUIMIENTO;
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

    private BigDecimal nullToZero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
