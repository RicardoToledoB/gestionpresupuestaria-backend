package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.ceropapel.CeroPapelConfigDto;
import cl.dssm.presupuesto.dto.ceropapel.CeroPapelConnectionStatusDto;
import cl.dssm.presupuesto.dto.ceropapel.CeroPapelPurchaseRequestDto;
import cl.dssm.presupuesto.dto.ceropapel.CeroPapelPurchaseRequestPageDto;
import cl.dssm.presupuesto.service.CeroPapelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ceropapel")
@RequiredArgsConstructor
public class CeroPapelController {
    private final CeroPapelService service;

    @GetMapping("/config")
    public CeroPapelConfigDto config() { return service.config(); }

    @GetMapping("/status")
    public CeroPapelConnectionStatusDto status() { return service.status(); }

    @GetMapping("/purchase-requests")
    public CeroPapelPurchaseRequestPageDto list(@RequestParam(defaultValue = "") String search,
                                                @RequestParam(required = false) String id,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.listPurchaseRequests(search, id, startDate, endDate, page, size);
    }

    @GetMapping("/purchase-requests/{id}")
    public CeroPapelPurchaseRequestDto detail(@PathVariable String id) { return service.getPurchaseRequest(id); }
}
