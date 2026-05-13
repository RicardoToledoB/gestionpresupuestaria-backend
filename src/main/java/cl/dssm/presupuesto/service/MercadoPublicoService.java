package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.MercadoPublicoPurchaseOrderDto;
import cl.dssm.presupuesto.exception.MercadoPublicoRateLimitException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class MercadoPublicoService {
    private static final String DEFAULT_BASE_URL = "https://api.mercadopublico.cl/servicios/v1/publico/ordenesdecompra.json";

    /**
     * Mercado Público devuelve 429 cuando detecta peticiones simultáneas. En vez de insistir muchas veces,
     * serializamos las consultas y aplicamos un cooldown global cuando aparece 429. Esto evita amplificar el bloqueo.
     */
    private static final long MIN_TIME_BETWEEN_EXTERNAL_CALLS_MS = 2_500L;
    private static final long RATE_LIMIT_COOLDOWN_SECONDS = 90L;
    private static final long SINGLE_RETRY_DELAY_MS = 8_000L;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private final Object mercadoPublicoGlobalLock = new Object();
    private final AtomicReference<Instant> cooldownUntil = new AtomicReference<>(Instant.EPOCH);
    private volatile Instant lastExternalCallAt = Instant.EPOCH;

    @Value("${app.mercado-publico.ticket:F8537A18-6766-4DEF-9E59-426B4FEE2844}")
    private String ticket;

    @Value("${app.mercado-publico.base-url:" + DEFAULT_BASE_URL + "}")
    private String baseUrl;

    @Value("${app.mercado-publico.cache-minutes:720}")
    private long cacheMinutes;

    public MercadoPublicoPurchaseOrderDto findPurchaseOrderByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode.isBlank()) {
            throw new IllegalArgumentException("Debe indicar un código de orden de compra válido.");
        }
        if (ticket == null || ticket.isBlank()) {
            throw new IllegalStateException("No existe ticket de Mercado Público configurado. Defina MERCADO_PUBLICO_TICKET en Railway.");
        }

        CacheEntry cached = cache.get(normalizedCode);
        if (cached != null && !cached.isExpired()) {
            return withCacheMetadata(cached, true, false);
        }

        Object perCodeLock = locks.computeIfAbsent(normalizedCode, key -> new Object());
        try {
            synchronized (perCodeLock) {
                CacheEntry cachedInsideLock = cache.get(normalizedCode);
                if (cachedInsideLock != null && !cachedInsideLock.isExpired()) {
                    return withCacheMetadata(cachedInsideLock, true, false);
                }

                Instant currentCooldown = cooldownUntil.get();
                if (Instant.now().isBefore(currentCooldown)) {
                    // Si tenemos un dato anterior, aunque esté vencido, lo mostramos como caché histórica.
                    if (cachedInsideLock != null) {
                        return withCacheMetadata(cachedInsideLock, true, true);
                    }
                    long seconds = cooldownSecondsRemaining();
                    throw new MercadoPublicoRateLimitException(
                            "Mercado Público está limitando temporalmente las consultas. Espere aproximadamente " + seconds + " segundos e intente nuevamente."
                    );
                }

                try {
                    MercadoPublicoPurchaseOrderDto dto = requestSerializedWithCooldown(normalizedCode);
                    CacheEntry newEntry = new CacheEntry(dto, Instant.now(), Instant.now().plus(Duration.ofMinutes(Math.max(cacheMinutes, 1))));
                    cache.put(normalizedCode, newEntry);
                    return withCacheMetadata(newEntry, false, false);
                } catch (MercadoPublicoRateLimitException ex) {
                    // Si Mercado Público limita nuevas consultas pero existe una consulta previa, devolvemos esa información.
                    // Esto evita que el usuario quede sin dato cuando la API externa está temporalmente restringida.
                    if (cachedInsideLock != null) {
                        return withCacheMetadata(cachedInsideLock, true, true);
                    }
                    throw ex;
                }
            }
        } finally {
            locks.remove(normalizedCode, perCodeLock);
        }
    }

    private MercadoPublicoPurchaseOrderDto requestSerializedWithCooldown(String normalizedCode) {
        synchronized (mercadoPublicoGlobalLock) {
            Instant currentCooldown = cooldownUntil.get();
            if (Instant.now().isBefore(currentCooldown)) {
                long seconds = cooldownSecondsRemaining();
                throw new MercadoPublicoRateLimitException(
                        "Mercado Público está limitando temporalmente las consultas. Espere aproximadamente " + seconds + " segundos e intente nuevamente."
                );
            }

            waitForMinimumSpacing();

            try {
                return callMercadoPublico(normalizedCode);
            } catch (MercadoPublicoRateLimitException firstRateLimit) {
                // Un único reintento controlado. Reintentar muchas veces empeora el bloqueo de Mercado Público.
                sleep(SINGLE_RETRY_DELAY_MS);
                try {
                    waitForMinimumSpacing();
                    return callMercadoPublico(normalizedCode);
                } catch (MercadoPublicoRateLimitException secondRateLimit) {
                    activateCooldown();
                    throw new MercadoPublicoRateLimitException(
                            "Mercado Público está limitando temporalmente las consultas por peticiones simultáneas. " +
                                    "El sistema pausó nuevas consultas por " + RATE_LIMIT_COOLDOWN_SECONDS + " segundos para proteger la integración. " +
                                    "Espere un momento e intente nuevamente.",
                            secondRateLimit
                    );
                }
            }
        }
    }

    private MercadoPublicoPurchaseOrderDto callMercadoPublico(String normalizedCode) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("codigo", normalizedCode)
                .queryParam("ticket", ticket.trim())
                .build()
                .toUriString();

        try {
            lastExternalCallAt = Instant.now();
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
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                activateCooldown();
                String detail = extractMercadoPublicoMessage(ex.getResponseBodyAsString());
                throw new MercadoPublicoRateLimitException(
                        "Mercado Público está limitando temporalmente las consultas" +
                                (detail == null || detail.isBlank() ? "." : ": " + detail) +
                                " Espere unos segundos e intente nuevamente.",
                        ex
                );
            }
            throw new IllegalStateException("No fue posible consultar la API de Mercado Público: " + friendlyHttpMessage(ex), ex);
        } catch (RestClientException ex) {
            if (isRateLimitMessage(ex.getMessage())) {
                activateCooldown();
                throw new MercadoPublicoRateLimitException(
                        "Mercado Público está limitando temporalmente las consultas por peticiones simultáneas. Espere unos segundos e intente nuevamente.",
                        ex
                );
            }
            throw new IllegalStateException("No fue posible consultar la API de Mercado Público: " + ex.getMessage(), ex);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible interpretar la respuesta de Mercado Público: " + ex.getMessage(), ex);
        }
    }

    private void waitForMinimumSpacing() {
        long elapsed = Duration.between(lastExternalCallAt, Instant.now()).toMillis();
        long remaining = MIN_TIME_BETWEEN_EXTERNAL_CALLS_MS - elapsed;
        if (remaining > 0) sleep(remaining);
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MercadoPublicoRateLimitException("La consulta a Mercado Público fue interrumpida durante la espera.", ex);
        }
    }

    private void activateCooldown() {
        cooldownUntil.set(Instant.now().plusSeconds(RATE_LIMIT_COOLDOWN_SECONDS));
    }

    private boolean isRateLimitMessage(String message) {
        if (message == null) return false;
        String text = message.toLowerCase();
        return text.contains("429") || text.contains("too many requests") || text.contains("peticiones simult");
    }

    private String friendlyHttpMessage(HttpStatusCodeException ex) {
        String apiMessage = extractMercadoPublicoMessage(ex.getResponseBodyAsString());
        if (apiMessage != null && !apiMessage.isBlank()) return apiMessage;
        return ex.getStatusCode() + " " + ex.getStatusText();
    }

    private String extractMercadoPublicoMessage(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            Map<String, Object> root = objectMapper.readValue(body, new TypeReference<>() {});
            Object message = root.get("Mensaje");
            if (message != null && !String.valueOf(message).isBlank()) return String.valueOf(message);
        } catch (Exception ignored) {}
        return body.length() > 280 ? body.substring(0, 280) + "..." : body;
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
                firstText(order, "NombreContacto", "NombreComprador", "CompradorNombreContacto", "ContactoComprador", "BuyerContactName"),
                firstText(supplier, "Nombre", "NombreProveedor", "RazonSocial", "Name"),
                firstText(supplier, "Rut", "RUT", "RutProveedor"),
                firstText(supplier, "Codigo", "CodigoProveedor", "Code"),
                sourceUrl,
                items,
                root,
                false,
                false,
                null,
                null,
                cooldownSecondsRemaining()
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
        return String.valueOf(code == null ? "" : code).replaceAll("\\s+", "").trim().toUpperCase();
    }


    private long cooldownSecondsRemaining() {
        Instant until = cooldownUntil.get();
        if (Instant.now().isAfter(until)) return 0L;
        return Math.max(Duration.between(Instant.now(), until).toSeconds(), 1L);
    }

    private MercadoPublicoPurchaseOrderDto withCacheMetadata(CacheEntry entry, boolean cacheHit, boolean cacheStale) {
        MercadoPublicoPurchaseOrderDto dto = entry.dto();
        return new MercadoPublicoPurchaseOrderDto(
                dto.code(),
                dto.name(),
                dto.description(),
                dto.stateCode(),
                dto.stateName(),
                dto.typeCode(),
                dto.typeName(),
                dto.currency(),
                dto.totalAmount(),
                dto.creationDate(),
                dto.sentDate(),
                dto.acceptedDate(),
                dto.cancellationDate(),
                dto.buyerName(),
                dto.buyerUnit(),
                dto.buyerRut(),
                dto.buyerContactName(),
                dto.supplierName(),
                dto.supplierRut(),
                dto.supplierCode(),
                dto.sourceUrl(),
                dto.items(),
                dto.raw(),
                cacheHit,
                cacheStale,
                entry.generatedAt().toString(),
                entry.expiresAt().toString(),
                cooldownSecondsRemaining()
        );
    }

    private record CacheEntry(MercadoPublicoPurchaseOrderDto dto, Instant generatedAt, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
