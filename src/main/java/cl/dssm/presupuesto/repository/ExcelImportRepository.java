package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.ExcelImport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcelImportRepository extends JpaRepository<ExcelImport, Long> {}
