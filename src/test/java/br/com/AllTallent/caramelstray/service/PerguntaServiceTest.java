package br.com.AllTallent.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        assertThrows(EntityNotFoundException.class,
                () -> questionService.criarPergunta(dto(null, null)));
    }

    @Test
    void createQuestionShouldSaveWhenTipoPerguntaIsNull() {
        // null tipo → tipo="" → isMultipla=false → else branch (covers null branch of ternary)
        stubSkillFound(); stubSave();
        assertNotNull(questionService.criarPergunta(dto(null, null)));
    }

    @Test
    void createQuestionShouldSaveWhenTypeIsNotMultipla() {
        // non-null tipo, not multipla → isMultipla=false (covers non-null ternary branch AND both OR=false)
        stubSkillFound(); stubSave();
        assertNotNull(questionService.criarPergunta(dto("ABERTA", null)));
    }

    @Test
    void createQuestionShouldEnterElseWhenAccentedMultiplaHasNullOpcoes() {
        // tipo contains "múltipla" → OR first=true (short-circuit); opcoes=null → compound AND=false → else
        stubSkillFound(); stubSave();
        assertNotNull(questionService.criarPergunta(dto("MÚLTIPLA ESCOLHA", null)));
    }

    @Test
    void createQuestionShouldEnterElseWhenUnaccentedMultiplaHasEmptyOpcoes() {
        // tipo contains "multipla" (second OR branch); opcoes=[] → !isEmpty=false → else
        stubSkillFound(); stubSave();
        assertNotNull(questionService.criarPergunta(dto("multipla escolha", List.of())));
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
