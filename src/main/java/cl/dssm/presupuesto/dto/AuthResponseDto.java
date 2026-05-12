package cl.dssm.presupuesto.dto;

import java.util.List;

public record AuthResponseDto(
        String accessToken,
        String tokenType,
        Long expiresInMs,
        String username,
        String fullName,
        List<String> roles
) {}
