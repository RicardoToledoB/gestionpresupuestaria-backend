package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.PurchaseOrderRequestDto;
import cl.dssm.presupuesto.entity.PurchaseOrder;
import cl.dssm.presupuesto.enums.PurchaseOrderStatus;
import cl.dssm.presupuesto.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PurchaseOrderManagementService {
    private final PurchaseOrderRepository repository;
    private final BudgetProgramRepository programRepository;
    private final ProviderRepository providerRepository;
    private final BudgetItemRepository itemRepository;
    private final CdpRepository cdpRepository;
    private final CdpRecalculationService recalculationService;
    private final AuditLogService auditLogService;

    @Transactional
    public PurchaseOrder create(PurchaseOrderRequestDto input) {
        if (repository.existsByOrderNumberAndDeletedAtIsNull(input.orderNumber())) {
            throw new IllegalArgumentException("Ya existe una OC con el número " + input.orderNumber());
        }
        PurchaseOrder po = new PurchaseOrder();
        apply(po, input);
        po = repository.save(po);
        recalculateRelated(po, null);
        auditLogService.register("ORDENES_COMPRA", "CREATE", "PurchaseOrder", po.getId(), po.getOrderNumber(), null, snapshot(po), "Creación manual de OC");
        return po;
    }

    @Transactional
    public PurchaseOrder update(Long id, PurchaseOrderRequestDto input) {
        PurchaseOrder po = repository.findById(id).orElseThrow();
        Long previousCdpId = po.getCdp() != null ? po.getCdp().getId() : null;
        String before = snapshot(po);
        if (!input.orderNumber().equalsIgnoreCase(po.getOrderNumber())) {
            if (repository.existsByOrderNumberAndDeletedAtIsNull(input.orderNumber())) {
                throw new IllegalArgumentException("Ya existe otra OC activa con el número " + input.orderNumber());
            }
        }
        apply(po, input);
        po = repository.save(po);
        recalculateRelated(po, previousCdpId);
        auditLogService.register("ORDENES_COMPRA", "UPDATE", "PurchaseOrder", po.getId(), po.getOrderNumber(), before, snapshot(po), "Actualización manual de OC");
        return po;
    }

    @Transactional
    public void softDelete(Long id) {
        PurchaseOrder po = repository.findById(id).orElseThrow();
        Long previousCdpId = po.getCdp() != null ? po.getCdp().getId() : null;
        String before = snapshot(po);
        po.setDeletedAt(LocalDateTime.now());
        po = repository.save(po);
        recalculateRelated(po, previousCdpId);
        auditLogService.register("ORDENES_COMPRA", "SOFT_DELETE", "PurchaseOrder", po.getId(), po.getOrderNumber(), before, snapshot(po), "Eliminación lógica de OC");
    }

    @Transactional
    public PurchaseOrder restore(Long id) {
        PurchaseOrder po = repository.findById(id).orElseThrow();
        String before = snapshot(po);
        po.setDeletedAt(null);
        po = repository.save(po);
        recalculateRelated(po, null);
        auditLogService.register("ORDENES_COMPRA", "RESTORE", "PurchaseOrder", po.getId(), po.getOrderNumber(), before, snapshot(po), "Recuperación de OC eliminada");
        return po;
    }

    private void apply(PurchaseOrder po, PurchaseOrderRequestDto input) {
        po.setOrderNumber(input.orderNumber());
        po.setOrderDate(input.orderDate());
        po.setSigfeFolio(input.sigfeFolio());
        po.setPurchaseRequestId(input.purchaseRequestId());
        po.setProductServiceReceptionDate(input.productServiceReceptionDate());
        po.setProgram(programRepository.findById(input.programId()).orElseThrow());
        po.setProvider(input.providerId() == null ? null : providerRepository.findById(input.providerId()).orElseThrow());
        po.setBudgetItem(input.budgetItemId() == null ? null : itemRepository.findById(input.budgetItemId()).orElseThrow());
        po.setCdp(input.cdpId() == null ? null : cdpRepository.findById(input.cdpId()).orElseThrow());
        po.setCommittedAmount(nullToZero(input.committedAmount()));
        po.setAdjustmentAmount(nullToZero(input.adjustmentAmount()));
        BigDecimal real = input.realAmount() == null ? nullToZero(input.committedAmount()).add(nullToZero(input.adjustmentAmount())) : input.realAmount();
        po.setRealAmount(nullToZero(real));
        po.setStatus(input.status() == null ? PurchaseOrderStatus.EMITIDA : input.status());
        po.setObservation(input.observation());
        po.setSubtitle(input.subtitle());
        if (Boolean.FALSE.equals(input.active())) po.setDeletedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(input.active())) po.setDeletedAt(null);
    }

    private void recalculateRelated(PurchaseOrder po, Long previousCdpId) {
        if (previousCdpId != null) recalculationService.recalculate(previousCdpId);
        if (po.getCdp() != null) recalculationService.recalculate(po.getCdp().getId());
    }

    private String snapshot(PurchaseOrder po) {
        return "OC=" + po.getOrderNumber()
                + "; solicitudCompra=" + (po.getPurchaseRequestId() != null ? po.getPurchaseRequestId() : "-")
                + "; recepcion=" + (po.getProductServiceReceptionDate() != null ? po.getProductServiceReceptionDate() : "-")
                + "; CDP=" + (po.getCdp() != null ? po.getCdp().getCdpNumber() : "-")
                + "; programa=" + (po.getProgram() != null ? po.getProgram().getName() : "-")
                + "; montoReal=" + po.getRealAmount();
    }

    private BigDecimal nullToZero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
