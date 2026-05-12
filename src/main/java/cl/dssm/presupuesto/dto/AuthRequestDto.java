package cl.dssm.presupuesto.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequestDto(
        @NotBlank String username,
        @NotBlank String password
) {}
