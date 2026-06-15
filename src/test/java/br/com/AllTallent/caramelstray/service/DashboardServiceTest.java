package br.com.AllTallent.caramelstray.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import br.com.AllTallent.dto.AreaQuantidadeDTO;
import br.com.AllTallent.dto.CompetenciaQuantidadeDTO;
import br.com.AllTallent.dto.DashboardResponseDTO;
import br.com.AllTallent.dto.MesQuantidadeProjection;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Avaliacao;
import br.com.AllTallent.model.AvaliacaoFuncionario;
import br.com.AllTallent.model.Competencia;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.RespostaColaborador;
import br.com.AllTallent.repository.AvaliacaoFuncionarioRepository;
import br.com.AllTallent.repository.AvaliacaoRepository;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.repository.RespostaColaboradorRepository;
import br.com.AllTallent.service.DashboardService;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private FuncionarioRepository employeeRepo;
    @Mock private AvaliacaoRepository evaluationRepo;
    @Mock private AvaliacaoFuncionarioRepository evaluationInstanceRepo;
    @Mock private RespostaColaboradorRepository collaboratorAnswerRepo;

    @InjectMocks private DashboardService dashboardService;

    private void stubSideCalls() {
        when(employeeRepo.countFuncionariosPorArea()).thenReturn(List.of());
        when(employeeRepo.countFuncionariosPorCompetencia()).thenReturn(List.of());
        when(evaluationInstanceRepo.findTopCompetenciasMaisAvaliadas(any())).thenReturn(List.of());
    }

    // getCollaboratorsByArea / getCollaboratorsBySkill / getTop5
    @Test
    void getCollaboratorsByAreaShouldDelegateToRepository() {
        List<AreaQuantidadeDTO> expected = List.of(mock(AreaQuantidadeDTO.class));
        when(employeeRepo.countFuncionariosPorArea()).thenReturn(expected);
        assertEquals(expected, dashboardService.getTotalColaboradoresArea());
    }

    @Test
    void getCollaboratorsBySkillShouldDelegateToRepository() {
        List<CompetenciaQuantidadeDTO> expected = List.of(mock(CompetenciaQuantidadeDTO.class));
        when(employeeRepo.countFuncionariosPorCompetencia()).thenReturn(expected);
        assertEquals(expected, dashboardService.getTotalColaboradoresCompetencia());
    }

    @Test
    void getTop5SkillsShouldPassPageOfFiveToRepository() {
        List<CompetenciaQuantidadeDTO> expected = List.of();
        when(evaluationInstanceRepo.findTopCompetenciasMaisAvaliadas(PageRequest.of(0, 5))).thenReturn(expected);
        assertEquals(expected, dashboardService.getTop5CompetenciasMaisAvaliadas());
    }

    // getDashboardData

    @Test
    void getDashboardWithAreaFilterShouldUseFilteredQueriesAndCalculateMeta() {
        MesQuantidadeProjection proj = mock(MesQuantidadeProjection.class);
        when(proj.getMes()).thenReturn("2025-01");
        when(proj.getQuantidade()).thenReturn(3L);

        when(employeeRepo.countByAreaCodigo(1)).thenReturn(20L);
        when(evaluationInstanceRepo.countTotalPendentesByArea(1)).thenReturn(3);
        when(evaluationInstanceRepo.countConcluidasNoMesByArea(any(), any(), eq(1))).thenReturn(10);
        when(evaluationInstanceRepo.countAprovadasNoMesByArea(any(), any(), eq(1))).thenReturn(8);
        when(employeeRepo.findEvolucaoMensalByArea(1)).thenReturn(List.of(proj));
        stubSideCalls();

        DashboardResponseDTO result = dashboardService.getDashboardData(1);

        assertEquals(20L, result.getTotalColaboradores());
        assertEquals(80.0, result.getMetaMensal());
        assertEquals(1, result.getEvolucaoMensal().size());
        verify(employeeRepo).countByAreaCodigo(1);
        verify(employeeRepo, never()).count();
    }

    @Test
    void getDashboardWithoutAreaFilterShouldUseGlobalQueriesAndZeroMetaWhenNoneCompleted() {
        when(employeeRepo.count()).thenReturn(50L);
        when(evaluationInstanceRepo.countTotalPendentes()).thenReturn(5);
        when(evaluationInstanceRepo.countConcluidasNoMes(any(), any())).thenReturn(0);
        when(evaluationInstanceRepo.countAprovadasNoMes(any(), any())).thenReturn(0);
        when(employeeRepo.findEvolucaoMensal()).thenReturn(List.of());
        stubSideCalls();

        DashboardResponseDTO result = dashboardService.getDashboardData(null);

        assertEquals(50L, result.getTotalColaboradores());
        assertEquals(0.0, result.getMetaMensal());
        verify(employeeRepo).count();
        verify(employeeRepo, never()).countByAreaCodigo(any());
    }

    @Test
    void getDashboardShouldReturnZeroMetaWhenConcluidosMesIsNull() {
        when(employeeRepo.count()).thenReturn(5L);
        when(evaluationInstanceRepo.countTotalPendentes()).thenReturn(0);
        when(evaluationInstanceRepo.countConcluidasNoMes(any(), any())).thenReturn(null);
        when(evaluationInstanceRepo.countAprovadasNoMes(any(), any())).thenReturn(null);
        when(employeeRepo.findEvolucaoMensal()).thenReturn(List.of());
        stubSideCalls();

        assertEquals(0.0, dashboardService.getDashboardData(null).getMetaMensal());
    }

    // generateSummary
    @Test
    void generateSummaryShouldCountStatusesAndIdentifyMissingDeliveries() {
        Avaliacao concluded = new Avaliacao(); concluded.setStatus("CONCLUIDO");
        Avaliacao pending = new Avaliacao(); pending.setStatus("PENDENTE");
        Avaliacao other = new Avaliacao(); other.setStatus("ABERTO");

        Funcionario alice = new Funcionario(); alice.setNomeCompleto("Alice");
        Funcionario bob = new Funcionario(); bob.setNomeCompleto("Bob");

        // instAlice: PENDING + no answers → appears in both pending and missingDelivery
        AvaliacaoFuncionario instAlice = mock(AvaliacaoFuncionario.class);
        lenient().when(instAlice.getResultadoStatus()).thenReturn("PENDENTE");
        lenient().when(instAlice.getFuncionario()).thenReturn(alice);
        lenient().when(instAlice.getCodigo()).thenReturn(1L);

        // instBob: COMPLETED + has answer → appears in neither list
        AvaliacaoFuncionario instBob = mock(AvaliacaoFuncionario.class);
        lenient().when(instBob.getResultadoStatus()).thenReturn("CONCLUIDO");
        lenient().when(instBob.getFuncionario()).thenReturn(bob);
        lenient().when(instBob.getCodigo()).thenReturn(2L);

        when(employeeRepo.findAll()).thenReturn(List.of(alice, bob));
        when(evaluationRepo.findAll()).thenReturn(List.of(concluded, pending, other));
        when(evaluationInstanceRepo.findAll()).thenReturn(List.of(instAlice, instBob));
        when(collaboratorAnswerRepo.findByAvaliacaoFuncionarioCodigo(1L)).thenReturn(List.of());
        when(collaboratorAnswerRepo.findByAvaliacaoFuncionarioCodigo(2L))
                .thenReturn(List.of(new RespostaColaborador()));

        Map<String, Object> result = dashboardService.gerarResumo();

        assertEquals(2L, result.get("totalColaboradores"));
        assertEquals(1L, result.get("avaliacoesConcluidas"));
        assertEquals(1L, result.get("avaliacoesPendentes"));
        @SuppressWarnings("unchecked")
        List<String> noDelivery = (List<String>) result.get("colaboradoresSemEntrega");
        assertEquals(List.of("Alice"), noDelivery);
    }

    // getDistributionByArea
    @Test
    void getDistributionByAreaShouldGroupByAreaNameAndFallbackForNullArea() {
        Area tech = new Area(); tech.setNome("Tech");
        Funcionario withArea = new Funcionario(); withArea.setArea(tech);
        Funcionario withoutArea = new Funcionario(); withoutArea.setArea(null);

        when(employeeRepo.findAll()).thenReturn(List.of(withArea, withoutArea));

        Map<String, Long> result = dashboardService.getDistribuicaoPorArea();

        assertEquals(1L, result.get("Tech"));
        assertEquals(1L, result.get("Sem área"));
    }

    // getDistributionBySkill
    @Test
    void getDistributionBySkillShouldHandleNamedBlankAndNullSkillNames() {
        // Competencia uses @EqualsAndHashCode(of = "codigo"), so each needs a distinct id.
        Competencia named = new Competencia(); named.setCodigo(1); named.setNome("Java");
        Competencia blank = new Competencia(); blank.setCodigo(2); blank.setNome("  ");
        Competencia nullName = new Competencia(); nullName.setCodigo(3); nullName.setNome(null);

        Funcionario f = mock(Funcionario.class);
        when(f.getCompetencias()).thenReturn(Set.of(named, blank, nullName));

        when(employeeRepo.findAll()).thenReturn(List.of(f));

        Map<String, Long> result = dashboardService.getDistribuicaoPorCompetencias();

        assertEquals(1L, result.get("Java"));
        assertEquals(2L, result.get("Sem nome"));
    }
}
