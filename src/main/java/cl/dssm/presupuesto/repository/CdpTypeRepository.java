package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.CdpType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface CdpTypeRepository extends JpaRepository<CdpType, Long> {
    Optional<CdpType> findByNameIgnoreCase(String name);

    @Query("""
        select e from CdpType e
        where (:includeDeleted = true or e.deletedAt is null)
          and (:search is null or :search = ''
            or lower(e.name) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.code,'')) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.description,'')) like lower(concat('%', :search, '%')))
        """)
    Page<CdpType> search(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);

    @Query("""
        select e from CdpType e
        where (:includeDeleted = true or e.deletedAt is null)
          and (:search is null or :search = ''
            or lower(e.name) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.code,'')) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.description,'')) like lower(concat('%', :search, '%')))
        order by e.name
        """)
    List<CdpType> exportRows(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted);
}
