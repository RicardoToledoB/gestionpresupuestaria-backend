package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.ceropapel.CeroPapelConfigDto;
import cl.dssm.presupuesto.dto.ceropapel.CeroPapelConnectionStatusDto;
import cl.dssm.presupuesto.dto.ceropapel.CeroPapelPurchaseRequestDto;
import cl.dssm.presupuesto.dto.ceropapel.CeroPapelPurchaseRequestPageDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CeroPapelService {
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ceropapel.base-url:https://ceropapel-integracion-api.dssm.cl}")
    private String baseUrl;

    @Value("${app.ceropapel.auth-path:/auth/login}")
    private String authPath;

    @Value("${app.ceropapel.purchase-requests-path:/purchase-requests/abastecimiento}")
    private String purchaseRequestsPath;

    @Value("${app.ceropapel.rut:}")
    private String rut;

    @Value("${app.ceropapel.password:}")
    private String password;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt;

    public CeroPapelConfigDto config() {
        return new CeroPapelConfigDto(baseUrl, authPath, purchaseRequestsPath, credentialsConfigured());
    }

    public CeroPapelConnectionStatusDto status() {
        if (!credentialsConfigured()) {
            return new CeroPapelConnectionStatusDto(false, false, baseUrl, purchaseRequestsPath,
                    "Debe configurar CERO_PAPEL_RUT y CERO_PAPEL_PASSWORD en variables de entorno del backend.");
        }
        try {
            String token = authenticate();
            return new CeroPapelConnectionStatusDto(true, token != null && !token.isBlank(), baseUrl, purchaseRequestsPath,
                    token != null && !token.isBlank() ? "Conexión autenticada correctamente." : "No fue posible obtener token de CeroPapel.");
        } catch (Exception ex) {
            return new CeroPapelConnectionStatusDto(true, false, baseUrl, purchaseRequestsPath,
                    "No fue posible autenticar contra CeroPapel: " + cleanMessage(ex.getMessage()));
        }
    }

    public CeroPapelPurchaseRequestPageDto listPurchaseRequests(String search, String startDate, String endDate, int page, int size) {
        if (!credentialsConfigured()) {
            return new CeroPapelPurchaseRequestPageDto(List.of(), page, size, 0, 0, false,
                    "Credenciales de CeroPapel no configuradas. Configure CERO_PAPEL_RUT y CERO_PAPEL_PASSWORD en Railway.", Map.of());
        }
        try {
            String token = authenticate();
            String effectiveStart = normalizeDate(startDate, LocalDate.now().withDayOfMonth(1).toString());
            String effectiveEnd = normalizeDate(endDate, LocalDate.now().toString());
            Map<String, Object> raw = fetchPurchaseRequestsRaw(token, effectiveStart, effectiveEnd);
            List<CeroPapelPurchaseRequestDto> all = extractList(raw).stream().map(this::mapPurchaseRequest).toList();
            List<CeroPapelPurchaseRequestDto> filtered = filterRequests(all, search);
            int safeSize = Math.max(1, size);
            int safePage = Math.max(0, page);
            int from = Math.min(safePage * safeSize, filtered.size());
            int to = Math.min(from + safeSize, filtered.size());
            List<CeroPapelPurchaseRequestDto> content = filtered.subList(from, to);
            long total = filtered.size();
            int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / (double) safeSize);
            Map<String, Object> envelope = new LinkedHashMap<>(raw);
            envelope.put("fechaInicioConsultada", effectiveStart);
            envelope.put("fechaFinConsultada", effectiveEnd);
            envelope.put("totalFiltradoSistema", total);
            return new CeroPapelPurchaseRequestPageDto(content, safePage, safeSize, total, totalPages, true,
                    content.isEmpty() ? "No se encontraron solicitudes de compra para el rango/criterio indicado." : "Solicitudes obtenidas correctamente desde CeroPapel.", envelope);
        } catch (HttpStatusCodeException ex) {
            return new CeroPapelPurchaseRequestPageDto(List.of(), page, size, 0, 0, false,
                    "No fue posible consultar CeroPapel (HTTP " + ex.getStatusCode().value() + "): " + cleanMessage(ex.getResponseBodyAsString()), Map.of());
        } catch (RestClientException ex) {
            return new CeroPapelPurchaseRequestPageDto(List.of(), page, size, 0, 0, false,
                    "No fue posible consultar CeroPapel: " + cleanMessage(ex.getMessage()), Map.of());
        }
    }

    public CeroPapelPurchaseRequestDto getPurchaseRequest(String id) {
        if (!credentialsConfigured()) {
            return new CeroPapelPurchaseRequestDto(id, null, null, "Credenciales no configuradas", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of(), "CeroPapel", Map.of());
        }
        try {
            String token = authenticate();
            Map<String, Object> raw = fetchPurchaseRequestDetailRaw(token, id);
            return mapPurchaseRequest(raw);
        } catch (Exception ex) {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("error", cleanMessage(ex.getMessage()));
            return new CeroPapelPurchaseRequestDto(id, null, null, "No fue posible obtener detalle", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of(), "CeroPapel", raw);
        }
    }

    private synchronized String authenticate() {
        if (cachedToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(30))) return cachedToken;

        List<String> attempted = new ArrayList<>();
        Exception lastError = null;

        for (String candidatePath : authPathCandidates()) {
            for (Map<String, Object> body : authPayloadCandidates()) {
                String url = resolveUrl(candidatePath);
                attempted.add(candidatePath);
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                    ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Object.class);
                    Map<String, Object> raw = asMap(response.getBody());
                    String token = findToken(raw);

                    if (token == null || token.isBlank()) {
                        // Algunas APIs devuelven el token dentro de data, usuario, result o payload.
                        token = findTokenDeep(raw);
                    }

                    if (token == null || token.isBlank()) {
                        lastError = new IllegalStateException("La respuesta de autenticación no incluyó token reconocible en " + candidatePath + ".");
                        continue;
                    }

                    cachedToken = token;
                    tokenExpiresAt = Instant.now().plusSeconds(extractLong(raw, "expiresIn", "expires_in", "expiresInSeconds", "expires", "expiraEn").orElse(3600L));
                    return cachedToken;
                } catch (HttpStatusCodeException ex) {
                    lastError = ex;
                    // Si la ruta no existe, probamos la siguiente sin cortar el flujo.
                    if (ex.getStatusCode().value() == 404 || ex.getStatusCode().value() == 405) continue;
                    // Si la ruta existe pero las credenciales/payload no calzan, igual probamos otro payload.
                    if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 422) continue;
                    throw new IllegalStateException("No fue posible autenticar contra CeroPapel en " + candidatePath + ": " + cleanMessage(ex.getResponseBodyAsString()), ex);
                } catch (Exception ex) {
                    lastError = ex;
                }
            }
        }

        String msg = "No fue posible autenticar contra CeroPapel. Rutas probadas: " + String.join(", ", attempted);
        if (lastError != null && lastError.getMessage() != null) msg += ". Último error: " + cleanMessage(lastError.getMessage());
        throw new IllegalStateException(msg, lastError);
    }

    private List<String> authPathCandidates() {
        List<String> candidates = new ArrayList<>();
        addUnique(candidates, authPath);
        // Rutas frecuentes en APIs NestJS/REST. El sistema prueba en orden y usa la primera que responda con token.
        addUnique(candidates, "/auth/login");
        addUnique(candidates, "/auth/signin");
        addUnique(candidates, "/auth/sign-in");
        addUnique(candidates, "/auth");
        addUnique(candidates, "/login");
        addUnique(candidates, "/usuarios/login");
        addUnique(candidates, "/users/login");
        addUnique(candidates, "/usuario/login");
        addUnique(candidates, "/autenticacion/login");
        return candidates;
    }

    private List<Map<String, Object>> authPayloadCandidates() {
        List<Map<String, Object>> payloads = new ArrayList<>();
        payloads.add(authPayload("rut", rut, "password", password));
        payloads.add(authPayload("username", rut, "password", password));
        payloads.add(authPayload("usuario", rut, "password", password));
        payloads.add(authPayload("run", rut, "password", password));
        payloads.add(authPayload("rut", rut.replaceAll("[^0-9kK]", ""), "password", password));
        return payloads;
    }

    private Map<String, Object> authPayload(String key1, String value1, String key2, String value2) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(key1, value1);
        body.put(key2, value2);
        return body;
    }

    private Map<String, Object> fetchPurchaseRequestsRaw(String token, String startDate, String endDate) {
        Exception lastError = null;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("startDate", startDate);
        body.put("endDate", endDate);

        for (String path : purchaseRequestPathCandidates()) {
            try {
                ResponseEntity<Object> response = restTemplate.exchange(resolveUrl(path), HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), Object.class);
                return asMap(response.getBody());
            } catch (HttpStatusCodeException ex) {
                lastError = ex;
                if (ex.getStatusCode().value() == 404 || ex.getStatusCode().value() == 405) continue;
                throw ex;
            } catch (Exception ex) {
                lastError = ex;
            }
        }
        throw new IllegalStateException("No fue posible encontrar ruta válida de solicitudes de compra. Rutas probadas: " + String.join(", ", purchaseRequestPathCandidates()) + (lastError != null ? ". Último error: " + cleanMessage(lastError.getMessage()) : ""), lastError);
    }

    private Map<String, Object> fetchPurchaseRequestDetailRaw(String token, String id) {
        Exception lastError = null;
        for (String path : purchaseRequestPathCandidates()) {
            try {
                URI uri = UriComponentsBuilder.fromHttpUrl(resolveUrl(path.replaceAll("/+$", "") + "/" + id)).build(true).toUri();
                ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Object.class);
                return asMap(response.getBody());
            } catch (HttpStatusCodeException ex) {
                lastError = ex;
                if (ex.getStatusCode().value() == 404 || ex.getStatusCode().value() == 405) continue;
                throw ex;
            } catch (Exception ex) {
                lastError = ex;
            }
        }
        throw new IllegalStateException("No fue posible encontrar ruta válida de detalle de solicitud. Último error: " + (lastError != null ? cleanMessage(lastError.getMessage()) : "Sin detalle"), lastError);
    }

    private List<String> purchaseRequestPathCandidates() {
        List<String> candidates = new ArrayList<>();
        addUnique(candidates, purchaseRequestsPath);
        addUnique(candidates, "/purchase-requests/abastecimiento");
        addUnique(candidates, "/solicitudes-compra");
        addUnique(candidates, "/solicitudes_compra");
        addUnique(candidates, "/solicitudesCompra");
        addUnique(candidates, "/solicitudes");
        addUnique(candidates, "/compras/solicitudes");
        addUnique(candidates, "/solicitudes/compras");
        addUnique(candidates, "/purchase-requests");
        return candidates;
    }

    private String normalizeDate(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private List<CeroPapelPurchaseRequestDto> filterRequests(List<CeroPapelPurchaseRequestDto> rows, String search) {
        if (search == null || search.isBlank()) return rows;
        String q = search.toLowerCase(Locale.ROOT).trim();
        return rows.stream()
                .filter(row -> contains(row.id(), q)
                        || contains(row.folio(), q)
                        || contains(row.codigo(), q)
                        || contains(row.titulo(), q)
                        || contains(row.descripcion(), q)
                        || contains(row.estado(), q)
                        || contains(row.solicitante(), q)
                        || contains(row.unidad(), q)
                        || contains(row.proveedorSugerido(), q))
                .toList();
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private void addUnique(List<String> list, String value) {
        if (value == null || value.isBlank()) return;
        String normalized = value.trim();
        if (!list.contains(normalized)) list.add(normalized);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null && !token.isBlank()) headers.setBearerAuth(token);
        return headers;
    }

    private CeroPapelPurchaseRequestDto mapPurchaseRequest(Map<String, Object> raw) {
        String id = first(raw, "id", "_id", "uuid", "solicitudId", "idSolicitud", "id_solicitud", "numeroSolicitud");
        String folio = first(raw, "folio", "numero", "numeroSolicitud", "nroSolicitud", "correlativo", "codigoDocumento", "id");
        String codigo = first(raw, "codigo", "codigoSolicitud", "idSolicitudCompra", "id_solicitud_compra");
        String titulo = first(raw, "titulo", "title", "nombre", "materia", "asunto", "glosa", "subject", "descripcionSolicitud");
        String descripcion = first(raw, "justification", "descripcion", "description", "detalle", "observacion", "observaciones", "observation1", "observation2", "fundamento");
        String estado = first(raw, "estado", "status", "state", "estadoSolicitud", "nombreEstado", "etapa");
        String solicitante = first(raw, "solicitante", "nombreSolicitante", "usuarioSolicitante", "requester", "creador", "nombreUsuario", "funcionario", "user_id");
        String unidad = first(raw, "unidad", "unidadSolicitante", "departamento", "area", "centroCosto", "location", "location_id");
        String fechaCreacion = first(raw, "fechaCreacion", "createdAt", "fecha", "fechaSolicitud", "fechaIngreso", "created_at");
        String fechaActualizacion = first(raw, "fechaActualizacion", "updatedAt", "fechaActualizacionEstado", "updated_at");
        String fechaEstimada = first(raw, "estimated_date", "fechaEstimada", "fecha_estimada", "fechaRequerida");
        String monto = first(raw, "monto", "montoEstimado", "montoTotal", "total", "valorEstimado", "estimated_amount", "amount");
        String proveedor = first(raw, "proveedor", "proveedorSugerido", "razonSocialProveedor", "nombreProveedor", "supplier");
        String urgencia = first(raw, "urgency", "urgencia", "priority", "prioridad");
        String usuarioId = first(raw, "user_id", "usuarioId", "idUsuario", "solicitante_id");
        String ubicacionId = first(raw, "location_id", "ubicacionId", "idUbicacion", "unidad_id");
        String programaId = first(raw, "programa_id", "programId", "programaId", "program_id");
        String observacion1 = first(raw, "observation1", "observacion1");
        String observacion2 = first(raw, "observation2", "observacion2");
        String registro = first(raw, "register", "registro");
        List<Map<String, Object>> movimientos = extractNestedList(raw, "movements_purchases", "movementsPurchases", "movimientos", "trazabilidad", "historial");
        return new CeroPapelPurchaseRequestDto(id, folio, codigo, titulo, descripcion, estado, solicitante, unidad, fechaCreacion, fechaActualizacion, fechaEstimada, monto, proveedor, urgencia, usuarioId, ubicacionId, programaId, observacion1, observacion2, registro, movimientos, "CeroPapel", raw);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractNestedList(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object item : list) out.add(asMap(item));
                return out;
            }
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object object) {
        if (object == null) return new LinkedHashMap<>();
        if (object instanceof Map<?, ?> map) return objectMapper.convertValue(map, new TypeReference<LinkedHashMap<String, Object>>() {});
        return objectMapper.convertValue(object, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Map<String, Object> raw) {
        Object candidate = raw;
        for (String key : List.of("result", "content", "data", "items", "results", "solicitudes", "solicitudesCompra", "records")) {
            Object value = raw.get(key);
            if (value instanceof List<?>) { candidate = value; break; }
            if (value instanceof Map<?, ?> nested) {
                for (String childKey : List.of("result", "content", "data", "items", "results", "solicitudes", "records")) {
                    Object child = ((Map<?, ?>) nested).get(childKey);
                    if (child instanceof List<?>) { candidate = child; break; }
                }
            }
        }
        if (candidate instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) out.add(asMap(item));
            return out;
        }
        if (!raw.isEmpty()) return List.of(raw);
        return List.of();
    }

    private Optional<Long> extractLong(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value instanceof Number number) return Optional.of(number.longValue());
            if (value instanceof String string && string.matches("\\d+")) return Optional.of(Long.parseLong(string));
        }
        return Optional.empty();
    }

    private String findToken(Map<String, Object> raw) {
        for (String key : List.of("access_token", "accessToken", "token", "jwt", "idToken", "access", "bearer")) {
            Object value = raw.get(key);
            if (value instanceof String token && !token.isBlank()) return token;
        }
        return null;
    }

    private String findTokenDeep(Map<String, Object> raw) {
        String direct = findToken(raw);
        if (direct != null) return direct;
        for (Object value : raw.values()) {
            if (value instanceof Map<?, ?> nested) {
                String token = findTokenDeep(asMap(nested));
                if (token != null) return token;
            }
        }
        return null;
    }

    private String first(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
        }
        for (String key : keys) {
            String found = findIgnoringCase(raw, key);
            if (found != null) return found;
        }
        return null;
    }

    private String findIgnoringCase(Map<String, Object> raw, String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(normalized) && entry.getValue() != null) {
                return String.valueOf(entry.getValue());
            }
        }
        return null;
    }

    private String resolveUrl(String path) {
        String trimmedBase = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        if (path == null || path.isBlank()) return trimmedBase;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return trimmedBase + (path.startsWith("/") ? path : "/" + path);
    }

    private boolean credentialsConfigured() {
        return rut != null && !rut.isBlank() && password != null && !password.isBlank();
    }

    private String cleanMessage(String message) {
        if (message == null) return "Sin detalle";
        return message.replaceAll("[\\r\\n\\t]+", " ").trim();
    }
}
