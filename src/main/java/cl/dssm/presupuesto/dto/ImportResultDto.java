package cl.dssm.presupuesto.dto;

public record ImportResultDto(
        Long importId,
        String status,
        Integer programsDetected,
        Integer movementsDetected,
        Integer cdpDetected,
        Integer purchaseOrdersDetected,
        Integer errorsDetected
) {}
