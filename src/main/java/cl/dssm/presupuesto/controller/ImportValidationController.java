package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.ImportValidationDto;
import cl.dssm.presupuesto.service.ImportValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/imports/validation")
@RequiredArgsConstructor
public class ImportValidationController {
    private final ImportValidationService service;

    @GetMapping
    public ImportValidationDto validate() {
        return service.validate();
    }
}
