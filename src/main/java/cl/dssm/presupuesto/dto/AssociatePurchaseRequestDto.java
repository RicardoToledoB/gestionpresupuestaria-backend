package cl.dssm.presupuesto.dto;

import jakarta.validation.constraints.NotBlank;

public record AssociatePurchaseRequestDto(@NotBlank String purchaseRequestId) {}
