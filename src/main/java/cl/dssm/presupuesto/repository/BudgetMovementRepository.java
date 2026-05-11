package cl.dssm.presupuesto.repository;

import cl.dssm.presupuesto.entity.BudgetMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetMovementRepository extends JpaRepository<BudgetMovement, Long> {}
