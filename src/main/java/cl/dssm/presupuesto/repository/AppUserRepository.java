package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.AppUser;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    @Query("""
        select e from AppUser e
        where (:includeDeleted = true or e.deletedAt is null)
          and (:search is null or :search = ''
            or lower(e.username) like lower(concat('%', :search, '%'))
            or lower(e.fullName) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.email,'')) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.rolesText,'')) like lower(concat('%', :search, '%')))
        """)
    Page<AppUser> search(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);

    @Query("""
        select e from AppUser e
        where (:includeDeleted = true or e.deletedAt is null)
          and (:search is null or :search = ''
            or lower(e.username) like lower(concat('%', :search, '%'))
            or lower(e.fullName) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.email,'')) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.rolesText,'')) like lower(concat('%', :search, '%')))
        order by e.fullName
        """)
    List<AppUser> exportRows(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted);
}
