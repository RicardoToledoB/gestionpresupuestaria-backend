package cl.dssm.presupuesto.dto;

import cl.dssm.presupuesto.entity.PurchaseOrder;
import cl.dssm.presupuesto.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderResponseDto(
        Long id,
        String orderNumber,
        LocalDate orderDate,
        String sigfeFolio,
        Long programId,
        String programName,
        Long providerId,
        String providerRut,
        String providerName,
        Long budgetItemId,
        String budgetItemCode,
        String budgetItemName,
        Long cdpId,
        String cdpNumber,
        BigDecimal committedAmount,
        BigDecimal adjustmentAmount,
        BigDecimal realAmount,
        PurchaseOrderStatus status,
        String observation,
        String subtitle,
        Boolean active,
        java.time.LocalDateTime deletedAt
) {
    public static PurchaseOrderResponseDto fromEntity(PurchaseOrder po) {
        return new PurchaseOrderResponseDto(
                po.getId(),
                po.getOrderNumber(),
                po.getOrderDate(),
                po.getSigfeFolio(),
                po.getProgram() != null ? po.getProgram().getId() : null,
                po.getProgram() != null ? po.getProgram().getName() : null,
                po.getProvider() != null ? po.getProvider().getId() : null,
                po.getProvider() != null ? po.getProvider().getRut() : null,
                po.getProvider() != null ? po.getProvider().getBusinessName() : null,
                po.getBudgetItem() != null ? po.getBudgetItem().getId() : null,
                po.getBudgetItem() != null ? po.getBudgetItem().getCode() : null,
                po.getBudgetItem() != null ? po.getBudgetItem().getName() : null,
                po.getCdp() != null ? po.getCdp().getId() : null,
                po.getCdp() != null ? po.getCdp().getCdpNumber() : null,
                po.getCommittedAmount(),
                po.getAdjustmentAmount(),
                po.getRealAmount(),
                po.getStatus(),
                po.getObservation(),
                po.getSubtitle(),
                po.getDeletedAt() == null,
                po.getDeletedAt()
        );
    }
}
