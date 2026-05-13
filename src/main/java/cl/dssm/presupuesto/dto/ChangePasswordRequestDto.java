package cl.dssm.presupuesto.dto;

public record ChangePasswordRequestDto(
        String currentPassword,
        String newPassword,
        String confirmPassword
) {}
