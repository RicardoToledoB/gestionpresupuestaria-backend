package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_users", indexes = @Index(name = "idx_app_user_username", columnList = "username", unique = true))
public class AppUser extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, length = 180)
    private String fullName;

    @Column(length = 180)
    private String email;

    @Column(length = 500)
    private String rolesText;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
