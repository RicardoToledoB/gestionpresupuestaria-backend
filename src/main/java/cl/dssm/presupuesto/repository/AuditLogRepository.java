package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
            select a from AuditLog a
            where :search is null or :search = ''
               or lower(a.module) like lower(concat('%', :search, '%'))
               or lower(a.action) like lower(concat('%', :search, '%'))
               or lower(a.entityName) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.businessKey, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.username, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.observation, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.previousValue, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.newValue, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.requestPath, '')) like lower(concat('%', :search, '%'))
            """)
    Page<AuditLog> search(@Param("search") String search, Pageable pageable);

    @Query("""
            select a from AuditLog a
            where :search is null or :search = ''
               or lower(a.module) like lower(concat('%', :search, '%'))
               or lower(a.action) like lower(concat('%', :search, '%'))
               or lower(a.entityName) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.businessKey, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.username, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.observation, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.previousValue, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.newValue, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(a.requestPath, '')) like lower(concat('%', :search, '%'))
            order by a.createdAt desc
            """)
    List<AuditLog> exportRows(@Param("search") String search);
    @Query("select count(a.id) from AuditLog a where lower(a.action) like lower(concat('%', :action, '%'))")
    long countActionLike(@Param("action") String action);

    @Query("select count(a.id) from AuditLog a where lower(a.module) like lower(concat('%', :module, '%'))")
    long countModuleLike(@Param("module") String module);

}
