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
        String fechaEstimada,
        String montoEstimado,
        String proveedorSugerido,
        String urgencia,
        String usuarioId,
        String ubicacionId,
        String programaId,
        String observacion1,
        String observacion2,
        String registro,
        List<Map<String, Object>> movimientosCompra,
        String origen,
        Map<String, Object> raw
) {}
