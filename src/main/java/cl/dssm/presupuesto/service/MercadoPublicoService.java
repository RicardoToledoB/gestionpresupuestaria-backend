package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.MercadoPublicoPurchaseOrderDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPublicoService {
    private static final String DEFAULT_BASE_URL = "https://api.mercadopublico.cl/servicios/v1/publico/ordenesdecompra.json";

    private final ObjectMapper objectMapper;

    @Value("${app.mercado-publico.ticket:F8537A18-6766-4DEF-9E59-426B4FEE2844}")
    private String ticket;

    @Value("${app.mercado-publico.base-url:" + DEFAULT_BASE_URL + "}")
    private String baseUrl;

    public MercadoPublicoPurchaseOrderDto findPurchaseOrderByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode.isBlank()) {
            throw new IllegalArgumentException("Debe indicar un código de orden de compra válido.");
        }
        if (ticket == null || ticket.isBlank()) {
            throw new IllegalStateException("No existe ticket de Mercado Público configurado. Defina MERCADO_PUBLICO_TICKET en Railway.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("codigo", normalizedCode)
                .queryParam("ticket", ticket.trim())
                .build()
                .toUriString();

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                throw new IllegalStateException("Mercado Público no entregó una respuesta válida para la OC " + normalizedCode + ".");
            }
            Map<String, Object> root = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            Map<String, Object> order = resolveFirstOrder(root);
            if (order.isEmpty()) {
                throw new IllegalStateException("No se encontró información para la OC " + normalizedCode + ".");
            }
            return toDto(normalizedCode, order, root);
        } catch (RestClientException ex) {
            throw new IllegalStateException("No fue posible consultar la API de Mercado Público: " + ex.getMessage(), ex);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible interpretar la respuesta de Mercado Público: " + ex.getMessage(), ex);
        }
    }

    private MercadoPublicoPurchaseOrderDto toDto(String requestedCode, Map<String, Object> order, Map<String, Object> root) {
        Map<String, Object> buyer = firstMap(order, "Comprador", "Buyer", "UnidadCompra", "Organismo");
        Map<String, Object> supplier = firstMap(order, "Proveedor", "Supplier");
        Map<String, Object> state = firstMap(order, "Estado", "EstadoOrdenCompra");
        Map<String, Object> type = firstMap(order, "Tipo", "TipoOrdenCompra");
        List<Map<String, Object>> items = resolveItems(order);

        String code = firstText(order, "Codigo", "CodigoExterno", "CodigoOrdenCompra", "Code");
        if (code == null || code.isBlank()) code = requestedCode;

        String sourceUrl = "https://www.mercadopublico.cl/PurchaseOrder/Modules/PO/DetailsPurchaseOrder.aspx?qs=" + code;

        return new MercadoPublicoPurchaseOrderDto(
                code,
                firstText(order, "Nombre", "Name", "Titulo"),
                firstText(order, "Descripcion", "Description", "Objeto"),
                firstText(order, "CodigoEstado", "EstadoCodigo", "Estado"),
                firstText(state, "Nombre", "Descripcion", "Estado", "Name"),
                firstText(order, "CodigoTipo", "TipoCodigo", "Tipo"),
                firstText(type, "Nombre", "Descripcion", "Tipo", "Name"),
                firstText(order, "Moneda", "Currency"),
                firstNumber(order, "Total", "MontoTotal", "TotalNeto", "Monto", "TotalOrden"),
                firstText(order, "FechaCreacion", "FechaCreacionOC", "CreationDate"),
                firstText(order, "FechaEnvio", "FechaEnvioProveedor", "SentDate"),
                firstText(order, "FechaAceptacion", "AcceptedDate"),
                firstText(order, "FechaCancelacion", "CancellationDate"),
                firstText(buyer, "NombreOrganismo", "NombreUnidad", "Nombre", "RazonSocial", "Name"),
                firstText(buyer, "NombreUnidad", "Unidad", "DireccionUnidad"),
                firstText(buyer, "RutUnidad", "RutOrganismo", "Rut", "RUT"),
                firstText(supplier, "Nombre", "NombreProveedor", "RazonSocial", "Name"),
                firstText(supplier, "Rut", "RUT", "RutProveedor"),
                firstText(supplier, "Codigo", "CodigoProveedor", "Code"),
                sourceUrl,
                items,
                root
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFirstOrder(Map<String, Object> root) {
        Object listado = root.get("Listado");
        if (listado instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            return new LinkedHashMap<>((Map<String, Object>) first);
        }
        if (listado instanceof Map<?, ?> map) {
            Object oc = map.get("OrdenCompra");
            if (oc instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                return new LinkedHashMap<>((Map<String, Object>) first);
            }
            if (oc instanceof Map<?, ?> ocMap) return new LinkedHashMap<>((Map<String, Object>) ocMap);
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        Object oc = root.get("OrdenCompra");
        if (oc instanceof Map<?, ?> map) return new LinkedHashMap<>((Map<String, Object>) map);
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveItems(Map<String, Object> order) {
        Object items = order.get("Items");
        if (items instanceof Map<?, ?> map) {
            Object listado = map.get("Listado");
            if (listado instanceof List<?> list) return toMapList(list);
            Object item = map.get("Item");
            if (item instanceof List<?> list) return toMapList(list);
            if (item instanceof Map<?, ?> one) return List.of(new LinkedHashMap<>((Map<String, Object>) one));
        }
        if (items instanceof List<?> list) return toMapList(list);
        Object listado = order.get("ListadoItems");
        if (listado instanceof List<?> list) return toMapList(list);
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(List<?> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) result.add(new LinkedHashMap<>((Map<String, Object>) map));
        }
        return result;
    }

    private Map<String, Object> firstMap(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                map.forEach((k, v) -> result.put(String.valueOf(k), v));
                return result;
            }
        }
        return Map.of();
    }

    private String firstText(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
        }
        return null;
    }

    private BigDecimal firstNumber(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null) continue;
            if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
            String text = String.valueOf(value).replace("$", "").replace(".", "").replace(",", ".").trim();
            if (!text.isBlank()) {
                try { return new BigDecimal(text); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private String normalizeCode(String code) {
        return String.valueOf(code == null ? "" : code).trim().toUpperCase();
    }
}
