package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {
    Optional<Provider> findByRut(String rut);

    @Query("""
            select p from Provider p
            where (:includeDeleted = true or p.deletedAt is null)
              and (:search is null or :search = ''
               or lower(p.rut) like lower(concat('%', :search, '%'))
               or lower(p.businessName) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.fantasyName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.email, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.phone, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Provider> search(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);

    @Query("""
            select p from Provider p
            where (:includeDeleted = true or p.deletedAt is null)
              and (:search is null or :search = ''
               or lower(p.rut) like lower(concat('%', :search, '%'))
               or lower(p.businessName) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.fantasyName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.email, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.phone, '')) like lower(concat('%', :search, '%')))
            order by p.businessName
            """)
    List<Provider> exportRows(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted);
}
