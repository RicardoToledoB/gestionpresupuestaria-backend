package cl.dssm.presupuesto.dto;

public record AuditStatsDto(
        long totalEvents,
        long createEvents,
        long updateEvents,
        long deleteEvents,
        long restoreEvents,
        long loginEvents,
        long importEvents
) {}
