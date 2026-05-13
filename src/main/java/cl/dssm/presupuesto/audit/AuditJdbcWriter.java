package cl.dssm.presupuesto.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditJdbcWriter {
    private static AuditJdbcWriter INSTANCE;
    private final JdbcTemplate jdbcTemplate;

    @jakarta.annotation.PostConstruct
    public void init() { INSTANCE = this; }

    public static AuditJdbcWriter getInstance() { return INSTANCE; }

    public void write(String module, String action, String entityName, Long entityId, String businessKey, String previousValue, String newValue, String observation) {
        LocalDateTime now = LocalDateTime.now();
        RequestData request = currentRequest();
        jdbcTemplate.update("""
                insert into audit_logs
                (created_at, updated_at, deleted_at, module, action, entity_name, entity_id, business_key, username, previous_value, new_value, observation, ip_address, user_agent, http_method, request_path)
                values (?, ?, null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                now,
                now,
                safe(module, 80),
                safe(action, 80),
                safe(entityName, 120),
                entityId,
                safe(businessKey, 120),
                safe(currentUsername(), 120),
                safe(previousValue, 3900),
                safe(newValue, 3900),
                safe(observation, 1200),
                safe(request.ipAddress(), 80),
                safe(request.userAgent(), 500),
                safe(request.method(), 20),
                safe(request.path(), 500)
        );
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) return "system";
        return auth.getName();
    }

    private RequestData currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            var request = attrs.getRequest();
            String forwardedFor = request.getHeader("X-Forwarded-For");
            String ip = forwardedFor != null && !forwardedFor.isBlank() ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
            return new RequestData(ip, request.getHeader("User-Agent"), request.getMethod(), request.getRequestURI());
        }
        return new RequestData(null, null, null, null);
    }

    private String safe(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private record RequestData(String ipAddress, String userAgent, String method, String path) {}
}
