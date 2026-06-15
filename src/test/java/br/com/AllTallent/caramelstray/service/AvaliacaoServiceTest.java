package br.com.AllTallent.caramelstray.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.dto.AvaliacaoRequestDTO;
import br.com.AllTallent.dto.RespostaColaboradorRequestDTO;
import br.com.AllTallent.dto.RevisaoDetalhadaDTO;
import br.com.AllTallent.dto.RevisaoSupervisorRequestDTO;
import br.com.AllTallent.exception.ResourceNotFoundException;
import br.com.AllTallent.exception.UnauthorizedActionException;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Avaliacao;
import br.com.AllTallent.model.AvaliacaoFuncionario;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.model.Pergunta;
import br.com.AllTallent.model.PerguntaOpcao;
import br.com.AllTallent.model.RespostaColaborador;
import br.com.AllTallent.repository.AvaliacaoFuncionarioRepository;
import br.com.AllTallent.repository.AvaliacaoRepository;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.repository.PerguntaOpcaoRepository;
import br.com.AllTallent.repository.PerguntaRepository;
import br.com.AllTallent.repository.RespostaColaboradorRepository;
import br.com.AllTallent.service.AvaliacaoService;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock private AvaliacaoRepository evaluationRepository;
    @Mock private FuncionarioRepository employeeRepository;
    @Mock private PerguntaRepository questionRepository;
    @Mock private AvaliacaoFuncionarioRepository evaluationInstanceRepository;
    @Mock private RespostaColaboradorRepository collaboratorAnswerRepository;
    @Mock private PerguntaOpcaoRepository questionOptionRepository;

    @InjectMocks private AvaliacaoService evaluationService;

    private SecurityContext securityContext;
    private Authentication authentication;
    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    private Area area;
    private Funcionario targetEmployee;
    private Avaliacao evaluation;
    private AvaliacaoFuncionario evaluationInstance;

    // Helpers
    private CustomUserDetails user(Integer id, Integer areaId, String... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r)).toList();
        CustomUserDetails ud = mock(CustomUserDetails.class);
        lenient().when(ud.getCodigo()).thenReturn(id);
        lenient().when(ud.getAreaId()).thenReturn(areaId);
        lenient().when(ud.getAuthorities()).thenAnswer(inv -> authorities);
        return ud;
    }

    private void login(CustomUserDetails ud) {
        when(authentication.getPrincipal()).thenReturn(ud);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    private Funcionario employee(Integer id, Area a) {
        Funcionario f = new Funcionario(); f.setCodigo(id); f.setArea(a); return f;
    }

    private Pergunta question(long id) {
        Pergunta p = new Pergunta(); p.setCodigo(id); return p;
    }

    private void stubCreate(CustomUserDetails evaluator) {
        Integer evaluatorId = evaluator.getCodigo();
        when(employeeRepository.getReferenceById(evaluatorId))
                .thenReturn(employee(evaluatorId, area));
        when(questionRepository.findAllById(any())).thenReturn(List.of(question(1L)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(targetEmployee));
    }

    private void stubCreateWithSave(CustomUserDetails evaluator) {
        stubCreate(evaluator);
        when(evaluationRepository.save(any())).thenAnswer(inv -> {
            Avaliacao a = inv.getArgument(0); a.setCodigo(1); return a;
        });
    }

    private AvaliacaoRequestDTO dto(List<Integer> employees, List<Long> questions) {
        return new AvaliacaoRequestDTO("T", null, employees, questions);
    }

    // Setup / Teardown
    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        area = new Area(); area.setCodigo(1); area.setNome("Technology");

        Perfil collaboratorProfile = new Perfil(); collaboratorProfile.setCodigo(3);

        targetEmployee = new Funcionario();
        targetEmployee.setCodigo(99); targetEmployee.setNomeCompleto("Target");
        targetEmployee.setArea(area); targetEmployee.setPerfil(collaboratorProfile);

        Funcionario creator = employee(10, area);

        evaluation = new Avaliacao();
        evaluation.setCodigo(1); evaluation.setTitulo("Test Evaluation");
        evaluation.setCriador(creator);
        evaluation.setPerguntas(new HashSet<>()); evaluation.setInstanciasAvaliacao(new HashSet<>());

        evaluationInstance = new AvaliacaoFuncionario(targetEmployee, evaluation);
        evaluationInstance.setCodigo(100L); evaluationInstance.setResultadoStatus("PENDENTE");
    }

    @AfterEach
    void tearDown() { securityContextHolderMock.close(); }

    // getLoggedUser
    @Test
    void getUserDetailsShouldThrowWhenAuthenticationIsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.listarTodasAvaliacoes());
    }

    @Test
    void getUserDetailsShouldThrowWhenPrincipalIsNotCustomUserDetails() {
        when(authentication.getPrincipal()).thenReturn("anonymous");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.listarTodasAvaliacoes());
    }

    // createEvaluation + canEvaluate branches
    @Test
    void createEvaluationShouldSucceedManagerEvaluatingCollaborator() {
        CustomUserDetails manager = user(10, 1, "ROLE_GESTOR");
        login(manager); stubCreateWithSave(manager);
        assertNotNull(evaluationService.criarAvaliacaoCompleta(dto(List.of(99), List.of(1L))));
        verify(evaluationInstanceRepository).save(any(AvaliacaoFuncionario.class));
    }

    @Test
    void createEvaluationShouldThrowWhenQuestionNotFound() {
        CustomUserDetails manager = user(10, 1, "ROLE_GESTOR");
        login(manager);
        when(employeeRepository.getReferenceById(10)).thenReturn(employee(10, area));
        when(questionRepository.findAllById(any())).thenReturn(List.of(question(1L)));
        var request = dto(List.of(99), List.of(1L, 2L));
        assertThrows(EntityNotFoundException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationShouldThrowWhenEmployeeNotFound() {
        CustomUserDetails manager = user(10, 1, "ROLE_GESTOR");
        login(manager);
        when(employeeRepository.getReferenceById(10)).thenReturn(employee(10, area));
        when(questionRepository.findAllById(any())).thenReturn(List.of(question(1L)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(targetEmployee));
        var request = dto(List.of(99, 100), List.of(1L));
        assertThrows(EntityNotFoundException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationShouldThrowWhenManagerTargetsDifferentArea() {
        Area other = new Area(); other.setCodigo(2);
        CustomUserDetails manager = user(10, 2, "ROLE_GESTOR");
        login(manager);
        when(employeeRepository.getReferenceById(10)).thenReturn(employee(10, other));
        when(questionRepository.findAllById(any())).thenReturn(List.of(question(1L)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(targetEmployee));
        var request = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationShouldThrowWhenManagerEvaluatesManager() {
        Perfil managerProfile = new Perfil(); managerProfile.setCodigo(2);
        targetEmployee.setPerfil(managerProfile);
        CustomUserDetails manager = user(10, 1, "ROLE_GESTOR");
        login(manager); stubCreate(manager);
        var request = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationShouldThrowWhenUserEvaluatesHimself() {
        CustomUserDetails admin = user(99, 1, "ROLE_ADMIN");
        login(admin); stubCreate(admin);
        var request = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationShouldThrowWhenUserHasNoRole() {
        CustomUserDetails noRole = user(10, 1);
        login(noRole); stubCreate(noRole);
        var request = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationShouldThrowWhenTargetProfileIsNull() {
        targetEmployee.setPerfil(null);
        CustomUserDetails manager = user(10, 1, "ROLE_GESTOR");
        login(manager); stubCreate(manager);
        var request = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationShouldThrowWhenTargetAreaIsNull() {
        targetEmployee.setArea(null);
        CustomUserDetails manager = user(10, 1, "ROLE_GESTOR");
        login(manager); stubCreate(manager);
        var request = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationShouldThrowWhenEvaluatorAreaIdIsNull() {
        CustomUserDetails manager = user(10, null, "ROLE_GESTOR");
        login(manager); stubCreate(manager);
        var request = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationAdminCanEvaluateManager() {
        Perfil managerProfile = new Perfil(); managerProfile.setCodigo(2);
        targetEmployee.setPerfil(managerProfile);
        CustomUserDetails admin = user(10, 1, "ROLE_ADMIN");
        login(admin); stubCreateWithSave(admin);
        assertDoesNotThrow(() -> evaluationService.criarAvaliacaoCompleta(dto(List.of(99), List.of(1L))));
    }

    @Test
    void createEvaluationAdminCanEvaluateCollaborator() {
        CustomUserDetails admin = user(10, 1, "ROLE_ADMIN");
        login(admin); stubCreateWithSave(admin);
        assertDoesNotThrow(() -> evaluationService.criarAvaliacaoCompleta(dto(List.of(99), List.of(1L))));
    }

    @Test
    void createEvaluationAdminCannotEvaluateDirector() {
        Perfil director = new Perfil(); director.setCodigo(1);
        targetEmployee.setPerfil(director);
        CustomUserDetails admin = user(10, 1, "ROLE_ADMIN");
        login(admin); stubCreate(admin);
        var request = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.criarAvaliacaoCompleta(request));
    }

    @Test
    void createEvaluationAdminGestorCanEvaluateCollaborator() {
        CustomUserDetails adminGestor = user(10, 1, "ROLE_GESTOR", "ROLE_ADMIN");
        login(adminGestor); stubCreateWithSave(adminGestor);
        assertDoesNotThrow(() -> evaluationService.criarAvaliacaoCompleta(dto(List.of(99), List.of(1L))));
    }

    @Test
    void createEvaluationAdminCannotEvaluateTargetInDifferentArea() {
        CustomUserDetails admin = user(10, 2, "ROLE_ADMIN");
        login(admin);
        when(employeeRepository.getReferenceById(10)).thenReturn(employee(10, area));
        when(questionRepository.findAllById(any())).thenReturn(List.of(question(1L)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(targetEmployee));
        var req = dto(List.of(99), List.of(1L));
        assertThrows(UnauthorizedActionException.class,
                () -> evaluationService.criarAvaliacaoCompleta(req));
    }

    // listAllEvaluations
    @Test
    void listAllEvaluationsAdminShouldReturnSameAreaEvaluations() {
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationRepository.findAll()).thenReturn(List.of(evaluation));
        assertEquals(1, evaluationService.listarTodasAvaliacoes().size());
    }

    @Test
    void listAllEvaluationsAdminShouldFilterNullCreator() {
        login(user(10, 1, "ROLE_ADMIN"));
        Avaliacao noCreator = new Avaliacao(); noCreator.setCriador(null);
        when(evaluationRepository.findAll()).thenReturn(List.of(noCreator, evaluation));
        assertEquals(1, evaluationService.listarTodasAvaliacoes().size());
    }

    @Test
    void listAllEvaluationsAdminShouldFilterDifferentArea() {
        login(user(10, 2, "ROLE_ADMIN"));
        when(evaluationRepository.findAll()).thenReturn(List.of(evaluation));
        assertTrue(evaluationService.listarTodasAvaliacoes().isEmpty());
    }

    @Test
    void listAllEvaluationsAdminShouldFilterNullCreatorArea() {
        login(user(10, 1, "ROLE_ADMIN"));
        Avaliacao eval = new Avaliacao(); eval.setCriador(employee(10, null));
        when(evaluationRepository.findAll()).thenReturn(List.of(eval));
        assertTrue(evaluationService.listarTodasAvaliacoes().isEmpty());
    }

    @Test
    void listAllEvaluationsManagerShouldReturnOwnEvaluations() {
        login(user(10, 1, "ROLE_GESTOR")); // creator id=10
        when(evaluationRepository.findAll()).thenReturn(List.of(evaluation));
        assertEquals(1, evaluationService.listarTodasAvaliacoes().size());
    }

    @Test
    void listAllEvaluationsManagerShouldHideOtherCreatorEvaluations() {
        login(user(55, 1, "ROLE_GESTOR")); // creator is id=10
        when(evaluationRepository.findAll()).thenReturn(List.of(evaluation));
        assertTrue(evaluationService.listarTodasAvaliacoes().isEmpty());
    }

    @Test
    void listAllEvaluationsManagerShouldFilterNullCreator() {
        login(user(10, 1, "ROLE_GESTOR"));
        Avaliacao noCreator = new Avaliacao(); noCreator.setCriador(null);
        when(evaluationRepository.findAll()).thenReturn(List.of(noCreator));
        assertTrue(evaluationService.listarTodasAvaliacoes().isEmpty());
    }

    @Test
    void listAllEvaluationsManagerShouldFilterNullCreatorArea() {
        login(user(10, 1, "ROLE_GESTOR"));
        Avaliacao eval = new Avaliacao(); eval.setCriador(employee(10, null));
        when(evaluationRepository.findAll()).thenReturn(List.of(eval));
        assertTrue(evaluationService.listarTodasAvaliacoes().isEmpty());
    }

    @Test
    void listAllEvaluationsManagerShouldFilterEvaluationFromDifferentArea() {
        login(user(10, 2, "ROLE_GESTOR"));
        when(evaluationRepository.findAll()).thenReturn(List.of(evaluation)); // criador.area.codigo=1
        assertTrue(evaluationService.listarTodasAvaliacoes().isEmpty());
    }

    @Test
    void listAllEvaluationsShouldReturnEmptyForUserWithoutRole() {
        login(user(10, 1));
        when(evaluationRepository.findAll()).thenReturn(List.of(evaluation));
        assertTrue(evaluationService.listarTodasAvaliacoes().isEmpty());
    }

    // fetchDetailedEvaluation + validateAccess
    @Test
    void fetchDetailedEvaluationShouldReturnDTOForAdminInSameArea() {
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        assertNotNull(evaluationService.buscarAvaliacaoDetalhada(1));
    }

    @Test
    void fetchDetailedEvaluationShouldThrowWhenNotFound() {
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> evaluationService.buscarAvaliacaoDetalhada(999));
    }

    @Test
    void validateAccessShouldThrowWhenCreatorIsNull() {
        evaluation.setCriador(null);
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.buscarAvaliacaoDetalhada(1));
    }

    @Test
    void validateAccessShouldThrowWhenCreatorAreaIsNull() {
        evaluation.getCriador().setArea(null);
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.buscarAvaliacaoDetalhada(1));
    }

    @Test
    void validateAccessShouldThrowWhenDifferentArea() {
        login(user(10, 2, "ROLE_ADMIN"));
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.buscarAvaliacaoDetalhada(1));
    }

    @Test
    void validateAccessShouldThrowWhenManagerIsNotCreator() {
        login(user(55, 1, "ROLE_GESTOR")); // creator=10
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.buscarAvaliacaoDetalhada(1));
    }

    @Test
    void validateAccessShouldPassWhenManagerIsCreator() {
        login(user(10, 1, "ROLE_GESTOR")); // creator=10
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        assertDoesNotThrow(() -> evaluationService.buscarAvaliacaoDetalhada(1));
    }

    @Test
    void fetchDetailedEvaluationShouldAllowAdminGestorWhoIsNotCreator() {
        login(user(99, 1, "ROLE_GESTOR", "ROLE_ADMIN")); // id=99 ≠ creator(10), same area
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        assertDoesNotThrow(() -> evaluationService.buscarAvaliacaoDetalhada(1));
    }

    // fetchInstancesByEvaluation / fetchAnswersByInstance
    @Test
    void fetchInstancesByEvaluationShouldReturnList() {
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        when(evaluationInstanceRepository.findByAvaliacaoCodigo(1)).thenReturn(List.of(evaluationInstance));
        assertEquals(1, evaluationService.buscarInstanciasPorAvaliacao(1).size());
    }

    @Test
    void fetchInstancesByEvaluationShouldThrowWhenNotFound() {
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> evaluationService.buscarInstanciasPorAvaliacao(999));
    }

    @Test
    void fetchAnswersByInstanceShouldReturnAnswers() {
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        when(collaboratorAnswerRepository.findByAvaliacaoFuncionarioCodigo(100L)).thenReturn(List.of());
        assertNotNull(evaluationService.buscarRespostasPorInstancia(100L));
    }

    @Test
    void fetchAnswersByInstanceShouldThrowWhenNotFound() {
        login(user(10, 1, "ROLE_ADMIN"));
        when(evaluationInstanceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> evaluationService.buscarRespostasPorInstancia(999L));
    }

    // saveOrUpdateAnswer
    @Test
    void saveOrUpdateAnswerShouldSaveWithoutOption() {
        login(user(99, 1));
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L)));
        when(collaboratorAnswerRepository.findByFuncionarioAvaliacaoCodigoAndPerguntaCodigo(100L, 1L))
                .thenReturn(Optional.empty());
        when(collaboratorAnswerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(evaluationService.salvarOuAtualizarResposta(
                new RespostaColaboradorRequestDTO(100L, 1L, "Text", null)));
    }

    @Test
    void saveOrUpdateAnswerShouldSaveWithValidOption() {
        login(user(99, 1));
        Pergunta q = question(1L);
        PerguntaOpcao opt = new PerguntaOpcao(); opt.setCodigo(5L); opt.setPergunta(q);
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));
        when(questionOptionRepository.findById(5L)).thenReturn(Optional.of(opt));
        when(collaboratorAnswerRepository.findByFuncionarioAvaliacaoCodigoAndPerguntaCodigo(100L, 1L))
                .thenReturn(Optional.empty());
        when(collaboratorAnswerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertDoesNotThrow(() -> evaluationService.salvarOuAtualizarResposta(
                new RespostaColaboradorRequestDTO(100L, 1L, null, 5L)));
    }

    @Test
    void saveOrUpdateAnswerShouldThrowWhenOptionBelongsToWrongQuestion() {
        login(user(99, 1));
        Pergunta q1 = question(1L); 
        Pergunta q2 = question(2L);
        PerguntaOpcao opt = new PerguntaOpcao(); opt.setCodigo(5L); opt.setPergunta(q2);
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(questionOptionRepository.findById(5L)).thenReturn(Optional.of(opt));
        var req = new RespostaColaboradorRequestDTO(100L, 1L, null, 5L);
        assertThrows(IllegalArgumentException.class, () -> evaluationService.salvarOuAtualizarResposta(req));
    }

    @Test
    void saveOrUpdateAnswerShouldThrowWhenInstanceNotFound() {
        login(user(99, 1));
        when(evaluationInstanceRepository.findById(999L)).thenReturn(Optional.empty());
        var req = new RespostaColaboradorRequestDTO(999L, 1L, "t", null);
        assertThrows(EntityNotFoundException.class, () -> evaluationService.salvarOuAtualizarResposta(req));
    }

    @Test
    void saveOrUpdateAnswerShouldThrowWhenQuestionNotFound() {
        login(user(99, 1));
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());
        var req = new RespostaColaboradorRequestDTO(100L, 999L, "t", null);
        assertThrows(EntityNotFoundException.class, () -> evaluationService.salvarOuAtualizarResposta(req));
    }

    @Test
    void saveOrUpdateAnswerShouldThrowWhenOptionNotFound() {
        login(user(99, 1));
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L)));
        when(questionOptionRepository.findById(999L)).thenReturn(Optional.empty());
        var req = new RespostaColaboradorRequestDTO(100L, 1L, null, 999L);
        assertThrows(EntityNotFoundException.class, () -> evaluationService.salvarOuAtualizarResposta(req));
    }

    @Test
    void saveOrUpdateAnswerShouldThrowWhenWrongCollaborator() {
        login(user(55, 1)); // instance owner is 99
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        var req = new RespostaColaboradorRequestDTO(100L, 1L, "t", null);
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.salvarOuAtualizarResposta(req));
    }

    @Test
    void saveOrUpdateAnswerShouldUpdateWhenAnswerAlreadyExists() {
        login(user(99, 1));
        RespostaColaborador existing = new RespostaColaborador();
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L)));
        when(collaboratorAnswerRepository.findByFuncionarioAvaliacaoCodigoAndPerguntaCodigo(100L, 1L))
                .thenReturn(Optional.of(existing));
        when(collaboratorAnswerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(evaluationService.salvarOuAtualizarResposta(
                new RespostaColaboradorRequestDTO(100L, 1L, "Updated", null)));
        verify(collaboratorAnswerRepository).save(existing);
    }

    // saveManagerReview
    @Test
    void saveManagerReviewShouldSave() {
        login(user(10, 1, "ROLE_GESTOR"));
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        when(evaluationInstanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(evaluationService.salvarRevisaoSupervisor(100L,
                new RevisaoSupervisorRequestDTO("Good", "Keep it up", "APPROVED")));
        verify(evaluationInstanceRepository).save(evaluationInstance);
    }

    @Test
    void saveManagerReviewShouldThrowWhenInstanceNotFound() {
        login(user(10, 1, "ROLE_GESTOR"));
        when(evaluationInstanceRepository.findById(999L)).thenReturn(Optional.empty());
        var req = new RevisaoSupervisorRequestDTO(null, null, null);
        assertThrows(EntityNotFoundException.class, () -> evaluationService.salvarRevisaoSupervisor(999L, req));
    }

    @Test
    void saveManagerReviewShouldThrowWhenManagerCannotEvaluate() {
        login(user(10, 2, "ROLE_GESTOR")); // area=2; target is area=1
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        var req = new RevisaoSupervisorRequestDTO(null, null, null);
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.salvarRevisaoSupervisor(100L, req));
    }

    // fetchForAnswer
    @Test
    void fetchForAnswerShouldReturnDTOWhenUserOwnsInstance() {
        login(user(99, 1));
        evaluationInstance.setAvaliacao(evaluation);
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        assertNotNull(evaluationService.buscarParaResponder(100L));
    }

    @Test
    void fetchForAnswerShouldThrowWhenNotFound() {
        login(user(99, 1));
        when(evaluationInstanceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> evaluationService.buscarParaResponder(999L));
    }

    @Test
    void fetchForAnswerShouldThrowWhenWrongCollaborator() {
        login(user(55, 1));
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.buscarParaResponder(100L));
    }

    @Test
    void fetchForAnswerShouldThrowWhenBaseEvaluationIsNull() {
        login(user(99, 1));
        evaluationInstance.setAvaliacao(null);
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        assertThrows(IllegalStateException.class, () -> evaluationService.buscarParaResponder(100L));
    }

    // finalizeByCollaborator
    @Test
    void finalizeByCollaboratorShouldFinalizeWhenStatusIsPending() {
        login(user(99, 1));
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        evaluationService.finalizarPeloColaborador(100L);
        assertEquals("AGUARDANDO_REVISAO", evaluationInstance.getResultadoStatus());
        verify(evaluationInstanceRepository).save(evaluationInstance);
    }

    @Test
    void finalizeByCollaboratorShouldThrowWhenStatusIsNotPending() {
        login(user(99, 1));
        evaluationInstance.setResultadoStatus("AGUARDANDO_REVISAO");
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        assertThrows(IllegalStateException.class, () -> evaluationService.finalizarPeloColaborador(100L));
    }

    @Test
    void finalizeByCollaboratorShouldThrowWhenNotFound() {
        login(user(99, 1));
        when(evaluationInstanceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> evaluationService.finalizarPeloColaborador(999L));
    }

    @Test
    void finalizeByCollaboratorShouldThrowWhenWrongCollaborator() {
        login(user(55, 1));
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        assertThrows(UnauthorizedActionException.class, () -> evaluationService.finalizarPeloColaborador(100L));
    }

    // fetchPendingByEmployee / fetchForReview / fetchReviewData
    @Test
    void fetchPendingByEmployeeShouldReturnOnlyPending() {
        AvaliacaoFuncionario approved = new AvaliacaoFuncionario();
        approved.setResultadoStatus("APROVADO");
        when(evaluationInstanceRepository.findByFuncionarioCodigo(99))
                .thenReturn(List.of(evaluationInstance, approved));
        assertEquals(1, evaluationService.buscarPendentesPorFuncionario(99).size());
    }

    @Test
    void fetchPendingByEmployeeShouldReturnEmptyWhenNoPending() {
        when(evaluationInstanceRepository.findByFuncionarioCodigo(99)).thenReturn(List.of());
        assertTrue(evaluationService.buscarPendentesPorFuncionario(99).isEmpty());
    }

    @Test
    void fetchForReviewShouldReturnDTO() {
        evaluationInstance.setAvaliacao(evaluation); evaluationInstance.setRespostas(new HashSet<>());
        when(evaluationInstanceRepository.findById(100L)).thenReturn(Optional.of(evaluationInstance));
        assertNotNull(evaluationService.buscarParaRevisao(100L));
    }

    @Test
    void fetchForReviewShouldThrowWhenNotFound() {
        when(evaluationInstanceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> evaluationService.buscarParaRevisao(999L));
    }

    @Test
    void fetchReviewDataShouldReturnListWithoutSelectedOption() {
        Pergunta q = question(1L); q.setPergunta("How do you rate?");
        RespostaColaborador answer = new RespostaColaborador();
        answer.setPergunta(q); answer.setRespostaTexto("Great"); answer.setPerguntaOpcaoSelecionada(null);
        when(evaluationInstanceRepository.existsById(100L)).thenReturn(true);
        when(collaboratorAnswerRepository.findByAvaliacaoFuncionarioCodigo(100L)).thenReturn(List.of(answer));
        List<RevisaoDetalhadaDTO> result = evaluationService.buscarDadosRevisao(100L);
        assertEquals(1, result.size());
        assertNull(result.get(0).getOpcaoSelecionadaId());
    }

    @Test
    void fetchReviewDataShouldReturnListWithSelectedOption() {
        Pergunta q = question(1L); q.setPergunta("Rate 1-5");
        PerguntaOpcao opt = new PerguntaOpcao(); opt.setCodigo(7L);
        RespostaColaborador answer = new RespostaColaborador();
        answer.setPergunta(q); answer.setPerguntaOpcaoSelecionada(opt);
        when(evaluationInstanceRepository.existsById(100L)).thenReturn(true);
        when(collaboratorAnswerRepository.findByAvaliacaoFuncionarioCodigo(100L)).thenReturn(List.of(answer));
        List<RevisaoDetalhadaDTO> result = evaluationService.buscarDadosRevisao(100L);
        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getOpcaoSelecionadaId());
    }

    @Test
    void fetchReviewDataShouldThrowWhenNotFound() {
        when(evaluationInstanceRepository.existsById(999L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> evaluationService.buscarDadosRevisao(999L));
    }
}
