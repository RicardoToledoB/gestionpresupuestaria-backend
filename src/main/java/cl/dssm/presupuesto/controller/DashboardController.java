package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.DashboardSummaryDto;
import cl.dssm.presupuesto.dto.DashboardChartsDto;
import cl.dssm.presupuesto.dto.ProgramSummaryDto;
import cl.dssm.presupuesto.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryDto summary() {
        return dashboardService.summary();
    }

    @GetMapping("/programs")
    public List<ProgramSummaryDto> programs() {
        return dashboardService.programs();
    }

    @GetMapping("/charts")
    public DashboardChartsDto charts() {
        return dashboardService.charts();
    }
}
