package cl.dssm.presupuesto.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

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

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 500)
    private String rolesText;

    /** Fecha y hora del último acceso exitoso del usuario. */
    private java.time.LocalDateTime lastLoginAt;

    /** Si tiene valor, el usuario está bloqueado y no puede iniciar sesión. */
    private java.time.LocalDateTime lockedAt;

    /** Fecha y hora del último cambio o reseteo de contraseña. */
    private java.time.LocalDateTime lastPasswordChangeAt;

    /**
     * Campos transientes usados por el mantenedor de usuarios para seleccionar roles
     * desde el catálogo oficial, sin escribirlos manualmente.
     */
    @Transient
    private List<Long> roleIds;

    @Transient
    private List<String> roleNames;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
