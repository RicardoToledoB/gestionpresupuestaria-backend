package cl.dssm.presupuesto.audit;

import cl.dssm.presupuesto.entity.BaseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.*;

public final class AuditSerializationUtils {
    private AuditSerializationUtils() {}

    public static String snapshot(Object entity) {
        if (entity == null) return null;
        List<String> values = new ArrayList<>();
        for (Field field : fieldsOf(entity.getClass())) {
            if (shouldSkip(field)) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                values.add(field.getName() + "=" + valueToString(value));
            } catch (Exception ignored) {
                values.add(field.getName() + "=<no-access>");
            }
        }
        return String.join("; ", values);
    }

    public static String moduleName(Object entity) {
        String simple = entity.getClass().getSimpleName();
        return switch (simple) {
            case "BudgetProgram" -> "PROGRAMAS_PRESUPUESTARIOS";
            case "Provider" -> "PROVEEDORES";
            case "BudgetItem" -> "ITEMS_PRESUPUESTARIOS";
            case "CdpType" -> "TIPOS_CDP";
            case "PurchaseOrderState" -> "ESTADOS_OC";
            case "BudgetSubtitle" -> "SUBTITULOS_PRESUPUESTARIOS";
            case "Cdp" -> "CDP";
            case "PurchaseOrder" -> "ORDENES_COMPRA";
            case "BudgetMovement" -> "MOVIMIENTOS_PRESUPUESTARIOS";
            case "ExcelImport" -> "IMPORTACIONES";
            case "ExcelImportError" -> "IMPORTACIONES_ERRORES";
            case "AppUser" -> "USUARIOS";
            case "AppRole" -> "ROLES";
            default -> simple.toUpperCase(Locale.ROOT);
        };
    }

    public static String businessKey(Object entity) {
        List<String> candidates = List.of(
                "cdpNumber", "orderNumber", "rut", "businessName", "name", "code", "username", "roleName", "filename", "description", "status"
        );
        for (String candidate : candidates) {
            Object value = readField(entity, candidate);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
        }
        if (entity instanceof BaseEntity base && base.getId() != null) return entity.getClass().getSimpleName() + "#" + base.getId();
        return entity.getClass().getSimpleName();
    }

    public static boolean isSoftDelete(String previous, Object entity) {
        return previous != null && previous.contains("deletedAt=null") && !snapshot(entity).contains("deletedAt=null");
    }

    public static boolean isRestore(String previous, Object entity) {
        return previous != null && !previous.contains("deletedAt=null") && snapshot(entity).contains("deletedAt=null");
    }

    public static boolean changed(String previous, Object entity) {
        String current = snapshot(entity);
        return previous == null || !previous.equals(current);
    }

    private static Object readField(Object entity, String fieldName) {
        for (Field field : fieldsOf(entity.getClass())) {
            if (field.getName().equals(fieldName)) {
                try {
                    field.setAccessible(true);
                    return field.get(entity);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static List<Field> fieldsOf(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        fields.sort(Comparator.comparing(Field::getName));
        return fields;
    }

    private static boolean shouldSkip(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) return true;
        if (Modifier.isTransient(modifiers)) return true;
        if (field.isAnnotationPresent(jakarta.persistence.Transient.class)) return true;
        String name = field.getName();
        return name.equals("auditPreviousSnapshot") || name.equals("serialVersionUID");
    }

    private static String valueToString(Object value) {
        if (value == null) return "null";
        if (value instanceof BaseEntity base) return value.getClass().getSimpleName() + "#" + base.getId();
        if (value instanceof Collection<?> collection) return "Collection(size=" + collection.size() + ")";
        if (value instanceof Map<?, ?> map) return "Map(size=" + map.size() + ")";
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum<?> || value instanceof Temporal || value instanceof BigDecimal) {
            return String.valueOf(value);
        }
        return value.getClass().getSimpleName();
    }
}
