package cl.dssm.presupuesto.dto.ceropapel;

import java.util.List;
import java.util.Map;

public record CeroPapelPurchaseRequestDto(
        String id,
        String folio,
        String codigo,
        String titulo,
        String descripcion,
        String estado,
        String solicitante,
        String unidad,
        String fechaCreacion,
        String fechaActualizacion,
        String montoEstimado,
        String proveedorSugerido,
        String origen,
        Map<String, Object> raw
) {}
