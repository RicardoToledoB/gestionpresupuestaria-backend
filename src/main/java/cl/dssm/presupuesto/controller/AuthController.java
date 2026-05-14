package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.AuthRequestDto;
import cl.dssm.presupuesto.dto.AuthResponseDto;
import cl.dssm.presupuesto.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody AuthRequestDto request) {
        return authService.login(request);
    }
}
