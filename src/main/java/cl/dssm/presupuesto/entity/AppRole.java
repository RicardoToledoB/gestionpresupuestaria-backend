package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_roles", indexes = @Index(name = "idx_app_role_name", columnList = "name", unique = true))
public class AppRole extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
