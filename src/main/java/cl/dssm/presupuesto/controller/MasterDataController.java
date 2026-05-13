package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.MasterOptionDto;
import cl.dssm.presupuesto.entity.*;
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
    private final PurchaseOrderRepository purchaseOrderRepository;

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
        syncDetectedCdpTypes();
        return cdpTypeRepository.findAll().stream()
                .filter(t -> t.getDeletedAt() == null && Boolean.TRUE.equals(t.getActive()))
                .map(t -> new MasterOptionDto(t.getId(), t.getName(), t.getCode()))
                .sorted(java.util.Comparator.comparing(MasterOptionDto::label, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @GetMapping("/purchase-order-states")
    public List<MasterOptionDto> purchaseOrderStates() {
        syncDetectedPurchaseOrderStates();
        return purchaseOrderStateRepository.findAll().stream()
                .filter(s -> s.getDeletedAt() == null && Boolean.TRUE.equals(s.getActive()))
                .map(s -> new MasterOptionDto(s.getId(), s.getName(), s.getCode()))
                .sorted(java.util.Comparator.comparing(MasterOptionDto::label, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @GetMapping("/budget-subtitles")
    public List<MasterOptionDto> budgetSubtitles() {
        syncDetectedBudgetSubtitles();
        return budgetSubtitleRepository.findAll().stream()
                .filter(s -> s.getDeletedAt() == null && Boolean.TRUE.equals(s.getActive()))
                .map(s -> new MasterOptionDto(s.getId(), s.getCode(), s.getName()))
                .sorted(java.util.Comparator.comparing(MasterOptionDto::label, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private void syncDetectedCdpTypes() {
        cdpRepository.findAll().stream()
                .map(c -> c.getCdpType())
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .forEach(name -> cdpTypeRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> cdpTypeRepository.save(CdpType.builder()
                                .name(name)
                                .code(normalizeCode(name))
                                .description("Detectado automáticamente desde CDP importados")
                                .active(true)
                                .build())));
    }


    private void syncDetectedPurchaseOrderStates() {
        // Si existen OC importadas, el mantenedor se alimenta desde sus estados reales.
        // Los registros sin estado quedan normalizados como SIN_ESTADO / Sin Estado.
        purchaseOrderRepository.findAll().stream()
                .map(PurchaseOrder::getStatus)
                .filter(java.util.Objects::nonNull)
                .forEach(status -> purchaseOrderStateRepository.findByCodeIgnoreCase(status.name())
                        .orElseGet(() -> purchaseOrderStateRepository.save(PurchaseOrderState.builder()
                                .code(status.name())
                                .name(humanizeEnum(status.name()))
                                .description("Detectado automáticamente desde órdenes de compra")
                                .active(true)
                                .build())));

        purchaseOrderStateRepository.findByCodeIgnoreCase("SIN_ESTADO")
                .orElseGet(() -> purchaseOrderStateRepository.save(PurchaseOrderState.builder()
                        .code("SIN_ESTADO")
                        .name("Sin Estado")
                        .description("Estado por defecto para OC sin estado en planilla")
                        .active(true)
                        .build()));
    }

    private void syncDetectedBudgetSubtitles() {
        // El mantenedor de Subtítulos presupuestarios se alimenta desde la columna Subt. de la hoja OC.
        purchaseOrderRepository.findAll().stream()
                .map(PurchaseOrder::getSubtitle)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .forEach(code -> budgetSubtitleRepository.findByCodeIgnoreCase(code)
                        .orElseGet(() -> budgetSubtitleRepository.save(BudgetSubtitle.builder()
                                .code(code)
                                .name("Subtítulo " + code)
                                .description("Detectado automáticamente desde órdenes de compra")
                                .active(true)
                                .build())));

        budgetSubtitleRepository.findByCodeIgnoreCase("SIN_SUBTITULO")
                .orElseGet(() -> budgetSubtitleRepository.save(BudgetSubtitle.builder()
                        .code("SIN_SUBTITULO")
                        .name("Sin subtítulo")
                        .description("Opción por defecto para OC sin subtítulo")
                        .active(true)
                        .build()));
    }

    private String humanizeEnum(String value) {
        if (value == null || value.isBlank()) return "Sin Estado";
        String lower = value.toLowerCase().replace('_', ' ');
        return java.util.Arrays.stream(lower.split(" "))
                .filter(p -> !p.isBlank())
                .map(p -> p.substring(0, 1).toUpperCase() + p.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
    }

    private Long fakeId(String value) {
        return -1L * Math.abs((long) value.hashCode());
    }

}
