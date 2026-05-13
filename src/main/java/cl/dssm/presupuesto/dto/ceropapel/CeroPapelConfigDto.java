package cl.dssm.presupuesto.dto.ceropapel;

public record CeroPapelConfigDto(
        String baseUrl,
        String authPath,
        String purchaseRequestsPath,
        boolean credentialsConfigured
) {}
