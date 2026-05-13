package cl.dssm.presupuesto.dto;

public record ResetPasswordRequestDto(
        String newPassword,
        String confirmPassword
) {}
