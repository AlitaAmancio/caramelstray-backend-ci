package br.com.AllTallent.caramelstray.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.AllTallent.dto.OpcaoRequest;
import br.com.AllTallent.dto.PerguntaRequestDTO;
import br.com.AllTallent.model.Competencia;
import br.com.AllTallent.model.Pergunta;
import br.com.AllTallent.repository.CompetenciaRepository;
import br.com.AllTallent.repository.PerguntaRepository;
import br.com.AllTallent.service.PerguntaService;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PerguntaServiceTest {

    @Mock private PerguntaRepository questionRepository;
    @Mock private CompetenciaRepository skillRepository;

    @InjectMocks private PerguntaService questionService;

    // Helpers
    private Competencia skill() {
        Competencia c = new Competencia(); c.setCodigo(1); c.setNome("Java"); return c;
    }

    private void stubSkillFound() {
        when(skillRepository.findById(1)).thenReturn(Optional.of(skill()));
    }

    private void stubSave() {
        when(questionRepository.save(any())).thenAnswer(inv -> {
            Pergunta p = inv.getArgument(0);
            p.setCodigo(1L);
            if (p.getOpcoes() == null) p.setOpcoes(new HashSet<>());
            return p;
        });
    }

    private PerguntaRequestDTO dto(String tipo, List<OpcaoRequest> opcoes) {
        return new PerguntaRequestDTO("What is Java?", 1, tipo, opcoes);
    }

    // createQuestion
    @Test
    void createQuestionShouldThrowWhenSkillNotFound() {
        when(skillRepository.findById(1)).thenReturn(Optional.empty());
        var request = dto(null, null);
        assertThrows(EntityNotFoundException.class, () -> questionService.criarPergunta(request));
    }

    // null tipo, non-multipla tipo, multipla+null opcoes (accented), multipla+empty opcoes → all hit the else branch
    static Stream<Arguments> createQuestionElseBranchInputs() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("ABERTA", null),
                Arguments.of("MÚLTIPLA ESCOLHA", null),
                Arguments.of("multipla escolha", List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("createQuestionElseBranchInputs")
    void createQuestionShouldSaveForElseBranchInputs(String tipo, List<OpcaoRequest> opcoes) {
        stubSkillFound(); stubSave();
        assertNotNull(questionService.criarPergunta(dto(tipo, opcoes)));
    }

    @Test
    void createQuestionShouldProcessOptionsAndSkipNullAndBlankDescricao() {
        // isMultipla=true, non-empty opcoes → enters if block
        // covers all inner branches: null descricao (skip), blank descricao (skip), valid descricao (add)
        List<OpcaoRequest> options = List.of(
                new OpcaoRequest(null, false),
                new OpcaoRequest("   ", false),
                new OpcaoRequest("Option A", true)
        );
        stubSkillFound(); stubSave();
        assertNotNull(questionService.criarPergunta(dto("múltipla", options)));
        verify(questionRepository).save(argThat(p -> p.getOpcoes() != null && !p.getOpcoes().isEmpty()));
    }

    // listAll
    @Test
    void listAllShouldReturnMappedDTOs() {
        Pergunta p = new Pergunta(); p.setCodigo(1L); p.setPergunta("What?");
        when(questionRepository.findAll()).thenReturn(List.of(p));
        assertEquals(1, questionService.listarTodas().size());
    }

    // findById
    @Test
    void findByIdShouldReturnDTOWhenFound() {
        Pergunta p = new Pergunta(); p.setCodigo(1L); p.setPergunta("What?");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertNotNull(questionService.buscarPorId(1L));
    }

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> questionService.buscarPorId(99L));
    }

    // deleteQuestion
    @Test
    void deleteQuestionShouldDeleteWhenExists() {
        when(questionRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> questionService.deletarPergunta(1L));
        verify(questionRepository).deleteById(1L);
    }

    @Test
    void deleteQuestionShouldThrowWhenNotExists() {
        when(questionRepository.existsById(99L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> questionService.deletarPergunta(99L));
    }
}
