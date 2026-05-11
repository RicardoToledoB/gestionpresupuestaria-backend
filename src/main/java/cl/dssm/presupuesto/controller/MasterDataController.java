package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.MasterOptionDto;
import cl.dssm.presupuesto.enums.PurchaseOrderStatus;
import cl.dssm.presupuesto.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/master-data")
@RequiredArgsConstructor
public class MasterDataController {
    private final BudgetProgramRepository programRepository;
    private final ProviderRepository providerRepository;
    private final BudgetItemRepository itemRepository;
    private final CdpRepository cdpRepository;
    private final CdpTypeRepository cdpTypeRepository;
    private final PurchaseOrderStateRepository purchaseOrderStateRepository;
    private final BudgetSubtitleRepository budgetSubtitleRepository;

    @GetMapping("/programs")
    public List<MasterOptionDto> programs() {
        return programRepository.findAll().stream().filter(p -> p.getDeletedAt() == null)
                .map(p -> new MasterOptionDto(p.getId(), p.getName(), p.getCode()))
                .sorted(java.util.Comparator.comparing(MasterOptionDto::label, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @GetMapping("/providers")
    public List<MasterOptionDto> providers() {
        return providerRepository.findAll().stream().filter(p -> p.getDeletedAt() == null)
                .map(p -> new MasterOptionDto(p.getId(), p.getBusinessName(), p.getRut()))
                .sorted(java.util.Comparator.comparing(MasterOptionDto::label, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @GetMapping("/budget-items")
    public List<MasterOptionDto> budgetItems() {
        return itemRepository.findAll().stream().filter(i -> i.getDeletedAt() == null)
                .map(i -> new MasterOptionDto(i.getId(), i.getName(), i.getCode()))
                .sorted(java.util.Comparator.comparing(MasterOptionDto::label, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @GetMapping("/cdps")
    public List<MasterOptionDto> cdps() {
        return cdpRepository.findAll().stream().filter(c -> c.getDeletedAt() == null)
                .map(c -> new MasterOptionDto(c.getId(), c.getCdpNumber(), c.getDescription()))
                .sorted(java.util.Comparator.comparing(MasterOptionDto::label, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @GetMapping("/cdp-types")
    public List<MasterOptionDto> cdpTypes() {
        Map<String, MasterOptionDto> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        cdpTypeRepository.findAll().stream().filter(t -> t.getDeletedAt() == null)
                .forEach(t -> options.putIfAbsent(t.getName(), new MasterOptionDto(t.getId(), t.getName(), t.getCode())));
        cdpRepository.findAll().stream()
                .map(c -> c.getCdpType())
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .forEach(v -> options.putIfAbsent(v, new MasterOptionDto(fakeId(v), v, "Detectado en CDP")));
        return options.values().stream().toList();
    }

    @GetMapping("/purchase-order-states")
    public List<MasterOptionDto> purchaseOrderStates() {
        Map<String, MasterOptionDto> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Arrays.stream(PurchaseOrderStatus.values())
                .forEach(s -> options.putIfAbsent(s.name(), new MasterOptionDto(fakeId(s.name()), s.name(), "Estado estándar")));
        purchaseOrderStateRepository.findAll().stream().filter(s -> s.getDeletedAt() == null)
                .forEach(s -> options.putIfAbsent(s.getName(), new MasterOptionDto(s.getId(), s.getName(), s.getCode())));
        return options.values().stream().toList();
    }

    @GetMapping("/budget-subtitles")
    public List<MasterOptionDto> budgetSubtitles() {
        Map<String, MasterOptionDto> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        budgetSubtitleRepository.findAll().stream().filter(s -> s.getDeletedAt() == null)
                .forEach(s -> options.putIfAbsent(s.getCode(), new MasterOptionDto(s.getId(), s.getCode(), s.getName())));
        cdpRepository.findAll().stream()
                .map(c -> c.getBudgetItem())
                .filter(Objects::nonNull)
                .map(i -> i.getCode())
                .filter(v -> v != null && !v.isBlank())
                .forEach(v -> options.putIfAbsent(v, new MasterOptionDto(fakeId(v), v, "Detectado desde ítems CDP")));
        return options.values().stream().toList();
    }

    private Long fakeId(String value) {
        return -1L * Math.abs((long) value.hashCode());
    }

}
