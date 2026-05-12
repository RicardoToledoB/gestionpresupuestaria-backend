package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.Cdp;
import cl.dssm.presupuesto.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CdpRepository extends JpaRepository<Cdp, Long> {
    Optional<Cdp> findFirstByCdpNumberAndDeletedAtIsNullOrderByIdAsc(String cdpNumber);
    boolean existsByCdpNumberAndDeletedAtIsNull(String cdpNumber);

    @Query("select count(c.id) from Cdp c where c.deletedAt is null and c.alertStatus = :status")
    long countByAlertStatus(@Param("status") AlertStatus status);

    @Query("select count(c.id) from Cdp c where c.deletedAt is null")
    long countActive();

    @Query("select count(c.id) from Cdp c where c.deletedAt is null and c.program is null")
    long countWithoutProgram();

    @Query("select count(c.id) from Cdp c where c.deletedAt is null and c.provider is null")
    long countWithoutProvider();

    @Query("select coalesce(sum(c.realCdpAmount), 0) from Cdp c where c.deletedAt is null")
    BigDecimal sumRealCdpAmount();

    @Query("select coalesce(sum(c.executedAmount), 0) from Cdp c where c.deletedAt is null")
    BigDecimal sumExecutedAmount();

    @Query("select coalesce(sum(c.pendingBalance), 0) from Cdp c where c.deletedAt is null")
    BigDecimal sumPendingBalance();

    @Query("select coalesce(sum(c.possibleReleaseAmount), 0) from Cdp c where c.deletedAt is null")
    BigDecimal sumPossibleReleaseAmount();

    @Query("select coalesce(sum(c.realCdpAmount), 0) from Cdp c where c.deletedAt is null and c.program.id = :programId")
    BigDecimal sumRealCdpAmountByProgramId(@Param("programId") Long programId);

    @EntityGraph(attributePaths = {"program", "provider", "budgetItem"})
    List<Cdp> findTop20ByAlertStatusInOrderByPossibleReleaseAmountDesc(List<AlertStatus> statuses);

    @EntityGraph(attributePaths = {"program"})
    @Query("""
            select c from Cdp c
            where c.deletedAt is null
              and c.pendingBalance is not null
            order by c.pendingBalance desc
            """)
    List<Cdp> findTopPending(Pageable pageable);

    @EntityGraph(attributePaths = {"program", "provider", "budgetItem"})
    @Query("""
            select c from Cdp c
            left join c.program program
            left join c.provider provider
            left join c.budgetItem item
            where (:includeDeleted = true or c.deletedAt is null)
              and (:search is null or :search = ''
               or lower(c.cdpNumber) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.description, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.cdpType, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.tenderOrContract, '')) like lower(concat('%', :search, '%'))
               or lower(program.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(provider.rut, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(provider.businessName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(item.code, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(item.name, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Cdp> search(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);

    @EntityGraph(attributePaths = {"program", "provider", "budgetItem"})
    @Query("""
            select c from Cdp c
            left join c.program program
            left join c.provider provider
            left join c.budgetItem item
            where (:includeDeleted = true or c.deletedAt is null)
              and (:search is null or :search = ''
               or lower(c.cdpNumber) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.description, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.cdpType, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.tenderOrContract, '')) like lower(concat('%', :search, '%'))
               or lower(program.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(provider.rut, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(provider.businessName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(item.code, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(item.name, '')) like lower(concat('%', :search, '%')))
            order by c.cdpNumber
            """)
    List<Cdp> exportRows(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted);
}
