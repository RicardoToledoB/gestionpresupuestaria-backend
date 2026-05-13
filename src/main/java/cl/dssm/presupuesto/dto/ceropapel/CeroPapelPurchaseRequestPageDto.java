package cl.dssm.presupuesto.dto.ceropapel;

import java.util.List;
import java.util.Map;

public record CeroPapelPurchaseRequestPageDto(
        List<CeroPapelPurchaseRequestDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean authenticated,
        String message,
        Map<String, Object> raw
) {}
