package cl.dssm.presupuesto.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MercadoPublicoPurchaseOrderDto(
        String code,
        String name,
        String description,
        String stateCode,
        String stateName,
        String typeCode,
        String typeName,
        String currency,
        BigDecimal totalAmount,
        String creationDate,
        String sentDate,
        String acceptedDate,
        String cancellationDate,
        String buyerName,
        String buyerUnit,
        String buyerRut,
        String buyerContactName,
        String supplierName,
        String supplierRut,
        String supplierCode,
        String sourceUrl,
        List<Map<String, Object>> items,
        Map<String, Object> raw,
        boolean cacheHit,
        boolean cacheStale,
        String cacheGeneratedAt,
        String cacheExpiresAt,
        long cooldownSecondsRemaining
) {}
