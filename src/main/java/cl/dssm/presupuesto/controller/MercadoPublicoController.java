package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.MercadoPublicoPurchaseOrderDto;
import cl.dssm.presupuesto.service.MercadoPublicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mercado-publico")
@RequiredArgsConstructor
public class MercadoPublicoController {
    private final MercadoPublicoService service;

    @GetMapping("/purchase-orders/{code}")
    public MercadoPublicoPurchaseOrderDto findPurchaseOrder(@PathVariable String code) {
        return service.findPurchaseOrderByCode(code);
    }
}
