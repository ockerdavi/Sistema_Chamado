package com.projeto.sistema_chamado.chamado.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "historico_status")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusChamado statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusChamado statusNovo;

    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    protected HistoricoStatus() {
    }

    public HistoricoStatus(
            Chamado chamado,
            StatusChamado statusAnterior,
            StatusChamado statusNovo,
            Instant alteradoEm) {

        this.chamado = chamado;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.alteradoEm = alteradoEm;
    }

    public Long getId() {
        return id;
    }

    public Chamado getChamado() {
        return chamado;
    }

    public StatusChamado getStatusAnterior() {
        return statusAnterior;
    }

    public StatusChamado getStatusNovo() {
        return statusNovo;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }
}
