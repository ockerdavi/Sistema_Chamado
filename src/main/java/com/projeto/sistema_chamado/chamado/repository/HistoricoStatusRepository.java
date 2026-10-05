package com.projeto.sistema_chamado.chamado.repository;

import com.projeto.sistema_chamado.chamado.domain.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
}
