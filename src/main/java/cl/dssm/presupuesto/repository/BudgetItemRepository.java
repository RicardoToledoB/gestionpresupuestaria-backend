package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.BudgetItem;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {
    Optional<BudgetItem> findByCode(String code);

    @Query("""
        select e from BudgetItem e
        where (:includeDeleted = true or e.deletedAt is null)
          and (:search is null or :search = ''
            or lower(e.code) like lower(concat('%', :search, '%'))
            or lower(e.name) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.subtitle,'')) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.description,'')) like lower(concat('%', :search, '%')))
        """)
    Page<BudgetItem> search(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);

    @Query("""
        select e from BudgetItem e
        where (:includeDeleted = true or e.deletedAt is null)
          and (:search is null or :search = ''
            or lower(e.code) like lower(concat('%', :search, '%'))
            or lower(e.name) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.subtitle,'')) like lower(concat('%', :search, '%'))
            or lower(coalesce(e.description,'')) like lower(concat('%', :search, '%')))
        order by e.code
        """)
    List<BudgetItem> exportRows(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted);
}
