package com.projeto.sistema_chamado.chamado.domain;

import com.projeto.sistema_chamado.categoria.Categoria;
import com.projeto.sistema_chamado.usuario.Usuario;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chamado")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusChamado status = StatusChamado.ABERTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade prioridade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "aberto_em", nullable = false)
    private Instant abertoEm;

    @Column(name = "atribuido_em")
    private Instant atribuidoEm;

    @Column(name = "resolvido_em")
    private Instant resolvidoEm;

    @Column(name = "fechado_em")
    private Instant fechadoEm;

    protected Chamado() {
    }


public Chamado(
        String titulo,
        String descricao,
        Prioridade prioridade,
        Usuario solicitante,
        Categoria categoria,
        Instant abertoEm) {

    this.titulo = titulo;
    this.descricao = descricao;
    this.prioridade = prioridade;
    this.solicitante = solicitante;
    this.categoria = categoria;
    this.abertoEm = abertoEm;
}

public Long getId() {
    return id;
}

public String getTitulo() {
    return titulo;
}

public String getDescricao() {
    return descricao;
}

public StatusChamado getStatus() {
    return status;
}

public Prioridade getPrioridade() {
    return prioridade;
}

public Usuario getSolicitante() {
    return solicitante;
}

public Usuario getTecnico() {
    return tecnico;
}

public Categoria getCategoria() {
    return categoria;
}

public Instant getAbertoEm() {
    return abertoEm;
}

public Instant getAtribuidoEm() {
    return atribuidoEm;
}

public Instant getResolvidoEm() {
    return resolvidoEm;
}

public Instant getFechadoEm() {
    return fechadoEm;
}

public void atribuirTecnico(Usuario tecnico, Instant atribuidoEm) {
    this.tecnico = tecnico;

    if (this.atribuidoEm == null) {
        this.atribuidoEm = atribuidoEm;
    }
}

public void iniciarAtendimento() {
    this.status = StatusChamado.EM_ATENDIMENTO;
}

public void resolver(Instant resolvidoEm) {
    this.status = StatusChamado.RESOLVIDO;
    this.resolvidoEm = resolvidoEm;
}

public void fechar(Instant fechadoEm) {
    this.status = StatusChamado.FECHADO;
    this.fechadoEm = fechadoEm;
}
}

