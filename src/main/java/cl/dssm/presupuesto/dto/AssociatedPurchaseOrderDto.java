package cl.dssm.presupuesto.dto;

import cl.dssm.presupuesto.entity.PurchaseOrder;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AssociatedPurchaseOrderDto(
        Long id,
        String orderNumber,
        LocalDate orderDate,
        String sigfeFolio,
        String purchaseRequestId,
        String programName,
        String providerName,
        Long cdpId,
        String cdpNumber,
        BigDecimal realAmount,
        String status,
        Boolean active
) {
    public static AssociatedPurchaseOrderDto fromEntity(PurchaseOrder po) {
        return new AssociatedPurchaseOrderDto(
                po.getId(),
                po.getOrderNumber(),
                po.getOrderDate(),
                po.getSigfeFolio(),
                po.getPurchaseRequestId(),
                po.getProgram() != null ? po.getProgram().getName() : null,
                po.getProvider() != null ? po.getProvider().getBusinessName() : null,
                po.getCdp() != null ? po.getCdp().getId() : null,
                po.getCdp() != null ? po.getCdp().getCdpNumber() : null,
                po.getRealAmount(),
                po.getStatus() != null ? po.getStatus().name() : null,
                po.getDeletedAt() == null
        );
    }
}
