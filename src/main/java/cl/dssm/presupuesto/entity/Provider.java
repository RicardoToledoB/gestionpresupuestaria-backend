package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "providers", indexes = {
        @Index(name = "idx_provider_rut", columnList = "rut", unique = true)
})
public class Provider extends BaseEntity {
    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @NotBlank
    @Column(nullable = false, length = 250)
    private String businessName;

    @Column(length = 250)
    private String fantasyName;

    @Column(length = 500)
    private String lineOfBusiness;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
