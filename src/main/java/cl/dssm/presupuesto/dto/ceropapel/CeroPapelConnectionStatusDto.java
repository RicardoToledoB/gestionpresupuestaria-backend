package cl.dssm.presupuesto.dto.ceropapel;

public record CeroPapelConnectionStatusDto(
        boolean configured,
        boolean authenticated,
        String baseUrl,
        String purchaseRequestsPath,
        String message
) {}
