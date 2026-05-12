package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.FormalQuadratureDto;
import cl.dssm.presupuesto.service.FormalQuadratureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quadrature")
@RequiredArgsConstructor
public class FormalQuadratureController {
    private final FormalQuadratureService service;

    @GetMapping("/formal")
    public FormalQuadratureDto formal() {
        return service.quadrature();
    }
}
