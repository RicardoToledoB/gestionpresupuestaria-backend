package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.BudgetProgram;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetProgramRepository extends JpaRepository<BudgetProgram, Long> {
    Optional<BudgetProgram> findByNameIgnoreCase(String name);

    @Query("select coalesce(sum(p.currentBudget), 0) from BudgetProgram p where p.deletedAt is null")
    java.math.BigDecimal sumCurrentBudget();

    @Query("""
            select p from BudgetProgram p
            where (:includeDeleted = true or p.deletedAt is null)
              and (:search is null or :search = ''
               or lower(p.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.code, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.subtitle, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
            """)
    Page<BudgetProgram> search(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);

    @Query("""
            select p from BudgetProgram p
            where (:includeDeleted = true or p.deletedAt is null)
              and (:search is null or :search = ''
               or lower(p.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.code, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.subtitle, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
            order by p.name
            """)
    List<BudgetProgram> exportRows(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted);
}
