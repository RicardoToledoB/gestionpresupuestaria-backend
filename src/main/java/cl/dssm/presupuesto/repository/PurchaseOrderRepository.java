package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findFirstByOrderNumberAndDeletedAtIsNullOrderByIdAsc(String orderNumber);
    boolean existsByOrderNumberAndDeletedAtIsNull(String orderNumber);
    List<PurchaseOrder> findByPurchaseRequestIdAndDeletedAtIsNullOrderByOrderDateDescOrderNumberAsc(String purchaseRequestId);
    List<PurchaseOrder> findByCdpIdAndDeletedAtIsNullOrderByOrderDateDescOrderNumberAsc(Long cdpId);

    @Query("select count(po.id) from PurchaseOrder po where po.deletedAt is null")
    long countActive();

    @Query("select count(po.id) from PurchaseOrder po where po.deletedAt is null and po.cdp is null")
    long countWithoutCdp();

    @Query("select coalesce(sum(po.realAmount), 0) from PurchaseOrder po where po.deletedAt is null and po.cdp.id = :cdpId")
    BigDecimal sumRealAmountByCdpId(@Param("cdpId") Long cdpId);

    @EntityGraph(attributePaths = {"program", "provider", "budgetItem", "cdp"})
    @Query("""
            select po from PurchaseOrder po
            left join po.program program
            left join po.provider provider
            left join po.budgetItem item
            left join po.cdp cdp
            where (:includeDeleted = true or po.deletedAt is null)
              and (:search is null or :search = ''
               or lower(po.orderNumber) like lower(concat('%', :search, '%'))
               or lower(coalesce(po.sigfeFolio, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(po.purchaseRequestId, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(po.observation, '')) like lower(concat('%', :search, '%'))
               or lower(program.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(provider.rut, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(provider.businessName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(item.code, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(item.name, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(cdp.cdpNumber, '')) like lower(concat('%', :search, '%')))
            """)
    Page<PurchaseOrder> search(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);

    @EntityGraph(attributePaths = {"program", "provider", "budgetItem", "cdp"})
    @Query("""
            select po from PurchaseOrder po
            left join po.program program
            left join po.provider provider
            left join po.budgetItem item
            left join po.cdp cdp
            where (:includeDeleted = true or po.deletedAt is null)
              and (:search is null or :search = ''
               or lower(po.orderNumber) like lower(concat('%', :search, '%'))
               or lower(coalesce(po.sigfeFolio, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(po.purchaseRequestId, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(po.observation, '')) like lower(concat('%', :search, '%'))
               or lower(program.name) like lower(concat('%', :search, '%'))
               or lower(coalesce(provider.rut, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(provider.businessName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(item.code, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(item.name, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(cdp.cdpNumber, '')) like lower(concat('%', :search, '%')))
            order by po.orderDate desc, po.orderNumber
            """)
    List<PurchaseOrder> exportRows(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted);
}
