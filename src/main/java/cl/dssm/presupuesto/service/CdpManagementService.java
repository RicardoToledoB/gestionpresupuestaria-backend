package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.CdpRequestDto;
import cl.dssm.presupuesto.entity.Cdp;
import cl.dssm.presupuesto.repository.BudgetItemRepository;
import cl.dssm.presupuesto.repository.BudgetProgramRepository;
import cl.dssm.presupuesto.repository.CdpRepository;
import cl.dssm.presupuesto.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CdpManagementService {
    private final CdpRepository cdpRepository;
    private final BudgetProgramRepository programRepository;
    private final ProviderRepository providerRepository;
    private final BudgetItemRepository itemRepository;
    private final CdpRecalculationService recalculationService;
    private final AuditLogService auditLogService;

    @Transactional
    public Cdp create(CdpRequestDto input) {
        if (cdpRepository.existsByCdpNumberAndDeletedAtIsNull(input.cdpNumber())) {
            throw new IllegalArgumentException("Ya existe un CDP con el número " + input.cdpNumber());
        }
        Cdp cdp = new Cdp();
        apply(cdp, input);
        cdp = cdpRepository.save(cdp);
        cdp = recalculationService.recalculate(cdp);
        auditLogService.register("CDP", "CREATE", "Cdp", cdp.getId(), cdp.getCdpNumber(), null, snapshot(cdp), "Creación manual de CDP");
        return cdp;
    }

    @Transactional
    public Cdp update(Long id, CdpRequestDto input) {
        Cdp cdp = cdpRepository.findById(id).orElseThrow();
        String before = snapshot(cdp);
        if (!input.cdpNumber().equalsIgnoreCase(cdp.getCdpNumber())) {
            if (cdpRepository.existsByCdpNumberAndDeletedAtIsNull(input.cdpNumber())) {
                throw new IllegalArgumentException("Ya existe otro CDP activo con el número " + input.cdpNumber());
            }
        }
        apply(cdp, input);
        cdp = cdpRepository.save(cdp);
        cdp = recalculationService.recalculate(cdp);
        auditLogService.register("CDP", "UPDATE", "Cdp", cdp.getId(), cdp.getCdpNumber(), before, snapshot(cdp), "Actualización manual de CDP");
        return cdp;
    }

    @Transactional
    public void softDelete(Long id) {
        Cdp cdp = cdpRepository.findById(id).orElseThrow();
        String before = snapshot(cdp);
        cdp.setDeletedAt(LocalDateTime.now());
        cdp = cdpRepository.save(cdp);
        recalculationService.recalculate(cdp);
        auditLogService.register("CDP", "SOFT_DELETE", "Cdp", cdp.getId(), cdp.getCdpNumber(), before, snapshot(cdp), "Eliminación lógica de CDP");
    }

    @Transactional
    public Cdp restore(Long id) {
        Cdp cdp = cdpRepository.findById(id).orElseThrow();
        String before = snapshot(cdp);
        cdp.setDeletedAt(null);
        cdp = cdpRepository.save(cdp);
        cdp = recalculationService.recalculate(cdp);
        auditLogService.register("CDP", "RESTORE", "Cdp", cdp.getId(), cdp.getCdpNumber(), before, snapshot(cdp), "Recuperación de CDP eliminado");
        return cdp;
    }

    private void apply(Cdp cdp, CdpRequestDto input) {
        cdp.setCdpNumber(input.cdpNumber());
        cdp.setCdpDate(input.cdpDate());
        cdp.setProgram(programRepository.findById(input.programId()).orElseThrow());
        cdp.setProvider(input.providerId() == null ? null : providerRepository.findById(input.providerId()).orElseThrow());
        cdp.setBudgetItem(input.budgetItemId() == null ? null : itemRepository.findById(input.budgetItemId()).orElseThrow());
        cdp.setCdpType(input.cdpType());
        cdp.setTenderOrContract(input.tenderOrContract());
        cdp.setDescription(input.description());
        cdp.setCoverageStart(input.coverageStart());
        cdp.setCoverageEnd(input.coverageEnd());
        cdp.setCoverageMonths(nullToZero(input.coverageMonths()));
        cdp.setCdpAmount(nullToZero(input.cdpAmount()));
        cdp.setCdpAdjustment(nullToZero(input.cdpAdjustment()));
        BigDecimal real = input.realCdpAmount() == null ? nullToZero(input.cdpAmount()).add(nullToZero(input.cdpAdjustment())) : input.realCdpAmount();
        cdp.setRealCdpAmount(nullToZero(real));
        cdp.setExpectedPercent(nullToZero(input.expectedPercent()));
        cdp.setObservation(input.observation());
        if (Boolean.FALSE.equals(input.active())) cdp.setDeletedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(input.active())) cdp.setDeletedAt(null);
    }

    private String snapshot(Cdp c) {
        return "CDP=" + c.getCdpNumber() + "; programa=" + (c.getProgram() != null ? c.getProgram().getName() : "-") + "; montoReal=" + c.getRealCdpAmount() + "; ejecutado=" + c.getExecutedAmount() + "; saldo=" + c.getPendingBalance();
    }

    private BigDecimal nullToZero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
