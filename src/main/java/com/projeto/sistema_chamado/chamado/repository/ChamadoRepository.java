package com.projeto.sistema_chamado.chamado.repository;

import com.projeto.sistema_chamado.chamado.domain.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
}
