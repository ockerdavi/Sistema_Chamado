package com.projeto.sistema_chamado.chamado.service;

import com.projeto.sistema_chamado.categoria.Categoria;
import com.projeto.sistema_chamado.categoria.CategoriaRepository;
import com.projeto.sistema_chamado.chamado.domain.Chamado;
import com.projeto.sistema_chamado.chamado.domain.HistoricoStatus;
import com.projeto.sistema_chamado.chamado.domain.Prioridade;
import com.projeto.sistema_chamado.chamado.domain.StatusChamado;
import com.projeto.sistema_chamado.chamado.repository.ChamadoRepository;
import com.projeto.sistema_chamado.chamado.repository.HistoricoStatusRepository;
import com.projeto.sistema_chamado.comentario.Comentario;
import com.projeto.sistema_chamado.comentario.ComentarioRepository;
import com.projeto.sistema_chamado.shared.exception.RegraNegocioException;
import com.projeto.sistema_chamado.shared.exception.RecursoNaoEncontradoException;
import com.projeto.sistema_chamado.usuario.Perfil;
import com.projeto.sistema_chamado.usuario.Usuario;
import com.projeto.sistema_chamado.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Consumer;

@Service
@Transactional
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final HistoricoStatusRepository historicoStatusRepository;
    private final ComentarioRepository comentarioRepository;
    private final Clock clock;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository,
            CategoriaRepository categoriaRepository,
            HistoricoStatusRepository historicoStatusRepository,
            ComentarioRepository comentarioRepository,
            Clock clock) {
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.comentarioRepository = comentarioRepository;
        this.clock = clock;
    }

    public Chamado abrir(String titulo, String descricao, Prioridade prioridade,
                         Long solicitanteId, Long categoriaId) {
        if (titulo == null || titulo.trim().length() < 5 || titulo.trim().length() > 120) {
            throw new RegraNegocioException("O título deve ter entre 5 e 120 caracteres.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new RegraNegocioException("A descrição é obrigatória.");
        }
        if (prioridade == null) {
            throw new RegraNegocioException("A prioridade é obrigatória.");
        }

        Usuario solicitante = buscarUsuario(solicitanteId);
        if (!solicitante.isAtivo()) {
            throw new RegraNegocioException("O solicitante precisa estar ativo.");
        }
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", categoriaId));
        if (!categoria.isAtiva()) {
            throw new RegraNegocioException("A categoria precisa estar ativa.");
        }

        return chamadoRepository.save(new Chamado(
                titulo.trim(), descricao.trim(), prioridade, solicitante, categoria, agora()));
    }

    public Chamado atribuir(Long chamadoId, Long tecnicoId) {
        Chamado chamado = buscarChamado(chamadoId);
        garantirNaoEncerrado(chamado);
        Usuario tecnico = buscarUsuario(tecnicoId);
        if (!tecnico.isAtivo() || tecnico.getPerfil() != Perfil.TECNICO) {
            throw new RegraNegocioException("A atribuição exige um usuário técnico ativo.");
        }
        chamado.atribuirTecnico(tecnico, agora());
        return chamadoRepository.save(chamado);
    }

    public Chamado iniciarAtendimento(Long chamadoId) {
        Chamado chamado = buscarChamado(chamadoId);
        if (chamado.getTecnico() == null) {
            throw new RegraNegocioException("É necessário atribuir um técnico antes do atendimento.");
        }
        alterarStatus(chamado, StatusChamado.ABERTO, StatusChamado.EM_ATENDIMENTO,
                instante -> chamado.iniciarAtendimento());
        return chamadoRepository.save(chamado);
    }

    public Comentario adicionarComentario(Long chamadoId, Long autorId, String texto) {
        Chamado chamado = buscarChamado(chamadoId);
        Usuario autor = buscarUsuario(autorId);
        if (!autor.isAtivo()) {
            throw new RegraNegocioException("O autor do comentário precisa estar ativo.");
        }
        if (texto == null || texto.isBlank()) {
            throw new RegraNegocioException("O comentário não pode estar vazio.");
        }
        garantirNaoEncerrado(chamado);
        return comentarioRepository.save(new Comentario(chamado, autor, texto.trim(), agora()));
    }

    public Chamado resolver(Long chamadoId, Long autorId, String solucao) {
        Chamado chamado = buscarChamado(chamadoId);
        if (solucao == null || solucao.isBlank()) {
            throw new RegraNegocioException("Informe um comentário de solução para resolver o chamado.");
        }
        Usuario autor = buscarUsuario(autorId);
        if (!autor.isAtivo()) {
            throw new RegraNegocioException("O autor do comentário precisa estar ativo.");
        }
        if (chamado.getStatus() != StatusChamado.EM_ATENDIMENTO) {
            throw new RegraNegocioException("Só é possível resolver um chamado em atendimento.");
        }

        comentarioRepository.save(new Comentario(chamado, autor, solucao.trim(), agora()));
        alterarStatus(chamado, StatusChamado.EM_ATENDIMENTO, StatusChamado.RESOLVIDO,
                chamado::resolver);
        return chamadoRepository.save(chamado);
    }

    public Chamado fechar(Long chamadoId) {
        Chamado chamado = buscarChamado(chamadoId);
        alterarStatus(chamado, StatusChamado.RESOLVIDO, StatusChamado.FECHADO,
                chamado::fechar);
        return chamadoRepository.save(chamado);
    }

    private void alterarStatus(Chamado chamado, StatusChamado esperado, StatusChamado novo,
                               Consumer<Instant> transicao) {
        if (chamado.getStatus() != esperado) {
            throw new RegraNegocioException(
                    "Transição inválida: esperado " + esperado + ", status atual " + chamado.getStatus() + ".");
        }
        Instant alteradoEm = agora();
        StatusChamado anterior = chamado.getStatus();
        transicao.accept(alteradoEm);
        historicoStatusRepository.save(new HistoricoStatus(chamado, anterior, novo, alteradoEm));
    }

    private void garantirNaoEncerrado(Chamado chamado) {
        if (chamado.getStatus() == StatusChamado.RESOLVIDO || chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Não é possível alterar um chamado resolvido ou fechado.");
        }
    }

    private Chamado buscarChamado(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado", id));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private Instant agora() {
        return Instant.now(clock);
    }
}
