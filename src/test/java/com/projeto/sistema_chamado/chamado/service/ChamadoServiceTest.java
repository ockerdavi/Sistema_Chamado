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
import com.projeto.sistema_chamado.usuario.Perfil;
import com.projeto.sistema_chamado.usuario.Usuario;
import com.projeto.sistema_chamado.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-05T12:00:00Z");

    @Mock private ChamadoRepository chamadoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private HistoricoStatusRepository historicoStatusRepository;
    @Mock private ComentarioRepository comentarioRepository;

    private ChamadoService service;
    private Usuario solicitante;
    private Usuario tecnico;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        service = new ChamadoService(chamadoRepository, usuarioRepository, categoriaRepository,
                historicoStatusRepository, comentarioRepository,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        solicitante = new Usuario("Ana", "ana@example.com", Perfil.SOLICITANTE);
        tecnico = new Usuario("Téo", "teo@example.com", Perfil.TECNICO);
        categoria = new Categoria("Rede");
    }

    @Test
    void abreChamadoComDadosValidosEDataUtcDoClock() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Chamado chamado = service.abrir(" Falha na rede ", "Sem acesso ao sistema", Prioridade.ALTA, 1L, 2L);

        assertEquals("Falha na rede", chamado.getTitulo());
        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        assertEquals(AGORA, chamado.getAbertoEm());
        verifyNoInteractions(historicoStatusRepository);
    }

    @Test
    void rejeitaAberturaComTituloForaDoLimite() {
        assertThrows(RegraNegocioException.class,
                () -> service.abrir("Ruim", "Descrição", Prioridade.MEDIA, 1L, 2L));
        verifyNoInteractions(usuarioRepository, categoriaRepository, chamadoRepository);
    }

    @Test
    void rejeitaSolicitanteOuCategoriaInativos() {
        solicitante.desativar();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));

        assertThrows(RegraNegocioException.class,
                () -> service.abrir("Falha de acesso", "Descrição", Prioridade.MEDIA, 1L, 2L));
        verifyNoInteractions(categoriaRepository, chamadoRepository);

        solicitante.ativar();
        categoria.desativar();
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
        assertThrows(RegraNegocioException.class,
                () -> service.abrir("Falha de acesso", "Descrição", Prioridade.MEDIA, 1L, 2L));
    }

    @Test
    void atribuiTecnicoERegistraSomenteAsTransicoesNoHistorico() {
        Chamado chamado = novoChamado();
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(tecnico));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        service.atribuir(10L, 3L);
        Chamado emAtendimento = service.iniciarAtendimento(10L);

        assertSame(tecnico, emAtendimento.getTecnico());
        assertEquals(AGORA, emAtendimento.getAtribuidoEm());
        assertEquals(StatusChamado.EM_ATENDIMENTO, emAtendimento.getStatus());
        ArgumentCaptor<HistoricoStatus> captor = ArgumentCaptor.forClass(HistoricoStatus.class);
        verify(historicoStatusRepository).save(captor.capture());
        assertEquals(StatusChamado.ABERTO, captor.getValue().getStatusAnterior());
        assertEquals(StatusChamado.EM_ATENDIMENTO, captor.getValue().getStatusNovo());
        assertEquals(AGORA, captor.getValue().getAlteradoEm());
    }

    @Test
    void rejeitaAtribuicaoParaUsuarioQueNaoSejaTecnicoAtivo() {
        Chamado chamado = novoChamado();
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(solicitante));

        assertThrows(RegraNegocioException.class, () -> service.atribuir(10L, 3L));
        assertNull(chamado.getTecnico());

        tecnico.desativar();
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(tecnico));
        assertThrows(RegraNegocioException.class, () -> service.atribuir(10L, 4L));
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void resolveComComentarioRegistraComentarioEHistoricoEPermiteFechamento() {
        Chamado chamado = novoChamado();
        chamado.atribuirTecnico(tecnico, AGORA);
        chamado.iniciarAtendimento();
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(tecnico));
        when(comentarioRepository.save(any(Comentario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Chamado resolvido = service.resolver(10L, 3L, "A conexão foi restabelecida");
        assertEquals(StatusChamado.RESOLVIDO, resolvido.getStatus());
        assertEquals(AGORA, resolvido.getResolvidoEm());
        Chamado fechado = service.fechar(10L);

        assertEquals(StatusChamado.FECHADO, fechado.getStatus());
        assertEquals(AGORA, fechado.getFechadoEm());
        verify(comentarioRepository).save(argThat(comentario ->
                comentario.getTexto().equals("A conexão foi restabelecida") && comentario.getAutor() == tecnico));
        verify(historicoStatusRepository, times(2)).save(any(HistoricoStatus.class));
    }

    @Test
    void rejeitaResolucaoSemSolucaoOuTransicaoForaDaOrdem() {
        Chamado chamado = novoChamado();
        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));

        assertThrows(RegraNegocioException.class, () -> service.resolver(10L, 3L, "  "));
        assertThrows(RegraNegocioException.class, () -> service.fechar(10L));
        verifyNoInteractions(historicoStatusRepository, comentarioRepository);
    }

    private Chamado novoChamado() {
        return new Chamado("Falha na rede", "Sem acesso", Prioridade.ALTA,
                solicitante, categoria, AGORA);
    }
}
