package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.ExcelImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExcelImportErrorRepository extends JpaRepository<ExcelImportError, Long> {
    List<ExcelImportError> findByExcelImportId(Long importId);
}
