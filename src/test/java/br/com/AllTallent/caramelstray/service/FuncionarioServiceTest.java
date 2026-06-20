package br.com.AllTallent.caramelstray.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.dto.CertificadoDTO;
import br.com.AllTallent.dto.CertificadoRequestDTO;
import br.com.AllTallent.dto.ExperienciaRequestDTO;
import br.com.AllTallent.dto.FuncionarioRequestDTO;
import br.com.AllTallent.dto.FuncionarioResponseDTO;
import br.com.AllTallent.exception.ResourceNotFoundException;
import br.com.AllTallent.exception.UnauthorizedActionException;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Competencia;
import br.com.AllTallent.model.Experiencia;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.FuncionarioCertificado;
import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.repository.AreaRepository;
import br.com.AllTallent.repository.CertificadoRepository;
import br.com.AllTallent.repository.CompetenciaRepository;
import br.com.AllTallent.repository.ExperienciaRepository;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.repository.PerfilRepository;
import br.com.AllTallent.service.FuncionarioService;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock private FuncionarioRepository employeeRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private PerfilRepository profileRepository;
    @Mock private CompetenciaRepository skillRepository;
    @Mock private ExperienciaRepository experienceRepository;
    @Mock private CertificadoRepository certificateRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private FuncionarioService employeeService;

    private SecurityContext securityContext;
    private Authentication authentication;
    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    private Area area;
    private Perfil profile;
    private Funcionario employee;

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
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    private FuncionarioRequestDTO dto(Integer areaId, Integer perfilId, Integer gestorId, String password) {
        return new FuncionarioRequestDTO("Alice", "a@test.com", "123", "555",
                password, areaId, perfilId, gestorId, "Engineer", "SP", "Bio");
    }

    private void stubSaveEmployee() {
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // Setup / Teardown
    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);

        area = new Area(); area.setCodigo(1); area.setNome("Tech");
        profile = new Perfil(); profile.setCodigo(3); profile.setNome("Collaborator");

        employee = new Funcionario();
        employee.setCodigo(10); employee.setNomeCompleto("Alice");
        employee.setEmail("alice@test.com");
        employee.setArea(area); employee.setPerfil(profile);
        employee.setCertificados(new HashSet<>());
        employee.setCompetencias(new HashSet<>());
        employee.setExperiencias(new HashSet<>());
    }

    @AfterEach
    void tearDown() { securityContextHolderMock.close(); }

    // listAll
    @Test
    void listAllShouldFindAllWhenTextIsNull() {
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        assertEquals(1, employeeService.listarTodos(null).size());
    }

    @Test
    void listAllShouldFindAllWhenTextIsBlank() {
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        assertEquals(1, employeeService.listarTodos("  ").size());
    }

    @Test
    void listAllShouldSearchByTextWhenTextIsProvided() {
        when(employeeRepository.buscarPorTexto("Alice")).thenReturn(List.of(employee));
        assertEquals(1, employeeService.listarTodos("Alice").size());
    }

    // findById
    @Test
    void findByIdShouldReturnDTOWhenFound() {
        when(employeeRepository.findById(10)).thenReturn(Optional.of(employee));
        assertNotNull(employeeService.buscarPorId(10));
    }

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(employeeRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> employeeService.buscarPorId(99));
    }

    // create
    @Test
    void createShouldMapAllPresentFieldsAndEncodePassword() {
        Funcionario manager = new Funcionario(); manager.setCodigo(5);
        when(areaRepository.findById(1)).thenReturn(Optional.of(area));
        when(profileRepository.findById(3)).thenReturn(Optional.of(profile));
        when(employeeRepository.findById(5)).thenReturn(Optional.of(manager));
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        stubSaveEmployee();

        FuncionarioResponseDTO result = employeeService.criar(dto(1, 3, 5, "secret"));

        assertNotNull(result);
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void createShouldSkipNullFieldsAndSetGestorToNull() {
        stubSaveEmployee();
        FuncionarioRequestDTO allNull = new FuncionarioRequestDTO(
                null, null, null, null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> employeeService.criar(allNull));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createShouldNotEncodePasswordWhenSenhaHashIsEmpty() {
        stubSaveEmployee();
        assertDoesNotThrow(() -> employeeService.criar(dto(null, null, null, "")));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createShouldThrowWhenAreaNotFound() {
        when(areaRepository.findById(1)).thenReturn(Optional.empty());
        var req = dto(1, null, null, null);
        assertThrows(ResourceNotFoundException.class, () -> employeeService.criar(req));
    }

    @Test
    void createShouldThrowWhenPerfilNotFound() {
        when(areaRepository.findById(1)).thenReturn(Optional.of(area));
        when(profileRepository.findById(3)).thenReturn(Optional.empty());
        var req = dto(1, 3, null, null);
        assertThrows(ResourceNotFoundException.class, () -> employeeService.criar(req));
    }

    @Test
    void createShouldThrowWhenGestorNotFound() {
        when(areaRepository.findById(1)).thenReturn(Optional.of(area));
        when(profileRepository.findById(3)).thenReturn(Optional.of(profile));
        when(employeeRepository.findById(5)).thenReturn(Optional.empty());
        var req = dto(1, 3, 5, null);
        assertThrows(ResourceNotFoundException.class, () -> employeeService.criar(req));
    }

    // update
    @Test
    void updateShouldReturnUpdatedDTOWhenFound() {
        when(employeeRepository.findById(10)).thenReturn(Optional.of(employee));
        stubSaveEmployee();
        assertNotNull(employeeService.atualizar(10,
                new FuncionarioRequestDTO(null, null, null, null, null, null, null, null, null, null, null)));
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        when(employeeRepository.findById(99)).thenReturn(Optional.empty());
        var req = dto(null, null, null, null);
        assertThrows(ResourceNotFoundException.class, () -> employeeService.atualizar(99, req));
    }

    // delete
    @Test
    void deleteShouldDeleteEmployeeWhenExists() {
        when(employeeRepository.existsById(10)).thenReturn(true);
        assertDoesNotThrow(() -> employeeService.deletar(10));
        verify(employeeRepository).deleteById(10);
    }

    @Test
    void deleteShouldThrowWhenNotExists() {
        when(employeeRepository.existsById(99)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> employeeService.deletar(99));
    }

    // findProfileById
    @Test
    void findProfileByIdShouldReturnDTOWhenFound() {
        when(employeeRepository.findByIdCompleto(10)).thenReturn(Optional.of(employee));
        assertNotNull(employeeService.buscarPerfilPorId(10));
    }

    @Test
    void findProfileByIdShouldThrowWhenNotFound() {
        when(employeeRepository.findByIdCompleto(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> employeeService.buscarPerfilPorId(99));
    }

    @Test
    void findCompleteEmployeeShouldReturnEmployeeWhenFound() {
        when(employeeRepository.findByIdCompleto(10)).thenReturn(Optional.of(employee));
        assertSame(employee, employeeService.buscarFuncionarioCompleto(10));
    }

    @Test
    void findCompleteEmployeeShouldThrowWhenNotFound() {
        when(employeeRepository.findByIdCompleto(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> employeeService.buscarFuncionarioCompleto(99));
    }

    @Test
    void listExperiencesByEmployeeShouldReturnDTOWhenFound() {
        when(employeeRepository.findByIdCompleto(10)).thenReturn(Optional.of(employee));
        assertNotNull(employeeService.listarExperienciasPorFuncionario(10));
    }

    @Test
    void listExperiencesByEmployeeShouldThrowWhenNotFound() {
        when(employeeRepository.findByIdCompleto(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.listarExperienciasPorFuncionario(99));
    }

    // addCertificate
    @Test
    void addCertificateShouldReturnDTOWhenEmployeeHasNullCertificates() {
        employee.setCertificados(null);
        when(employeeRepository.findById(10)).thenReturn(Optional.of(employee));
        when(certificateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CertificadoDTO result = employeeService.adicionarCertificado(10, new CertificadoRequestDTO("AWS"));
        assertNotNull(result);
    }

    @Test
    void addCertificateShouldReturnDTOWhenEmployeeHasExistingCertificates() {
        when(employeeRepository.findById(10)).thenReturn(Optional.of(employee));
        when(certificateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(employeeService.adicionarCertificado(10, new CertificadoRequestDTO("GCP")));
    }

    @Test
    void addCertificateShouldThrowWhenEmployeeNotFound() {
        when(employeeRepository.findById(99)).thenReturn(Optional.empty());
        var req = new CertificadoRequestDTO("AWS");
        assertThrows(EntityNotFoundException.class, () -> employeeService.adicionarCertificado(99, req));
    }

    // addExperience
    private ExperienciaRequestDTO expDto() {
        return new ExperienciaRequestDTO("Dev", "Acme", LocalDate.of(2020, Month.JANUARY, 1), null, "Built things");
    }

    @Test
    void addExperienceShouldReturnDTOWhenEmployeeHasNullExperiences() {
        employee.setExperiencias(null);
        when(employeeRepository.findById(10)).thenReturn(Optional.of(employee));
        when(experienceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(employeeService.adicionarExperiencia(10, expDto()));
    }

    @Test
    void addExperienceShouldReturnDTOWhenEmployeeHasExistingExperiences() {
        when(employeeRepository.findById(10)).thenReturn(Optional.of(employee));
        when(experienceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(employeeService.adicionarExperiencia(10, expDto()));
    }

    @Test
    void addExperienceShouldThrowWhenEmployeeNotFound() {
        when(employeeRepository.findById(99)).thenReturn(Optional.empty());
        var req = expDto();
        assertThrows(ResourceNotFoundException.class, () -> employeeService.adicionarExperiencia(99, req));
    }

    // updateExperience
    @Test
    void updateExperienceShouldUpdateAndReturnDTO() {
        Experiencia existing = new Experiencia(); existing.setFuncionario(employee);
        when(experienceRepository.findById(1)).thenReturn(Optional.of(existing));
        when(experienceRepository.save(any())).thenReturn(existing);
        assertNotNull(employeeService.atualizarExperiencia(1, expDto()));
    }

    @Test
    void updateExperienceShouldThrowWhenNotFound() {
        when(experienceRepository.findById(99)).thenReturn(Optional.empty());
        var req = expDto();
        assertThrows(ResourceNotFoundException.class, () -> employeeService.atualizarExperiencia(99, req));
    }

    // permissions
    @Test
    void canEditExperienceShouldReturnTrueWhenOwner() {
        Experiencia exp = new Experiencia(); exp.setFuncionario(employee); // employee.codigo = 10
        when(experienceRepository.findById(1)).thenReturn(Optional.of(exp));
        assertTrue(employeeService.usuarioPodeEditarExperiencia(1, 10));
    }

    @Test
    void canEditExperienceShouldReturnFalseWhenNotOwner() {
        Experiencia exp = new Experiencia(); exp.setFuncionario(employee);
        when(experienceRepository.findById(1)).thenReturn(Optional.of(exp));
        assertFalse(employeeService.usuarioPodeEditarExperiencia(1, 99));
    }

    @Test
    void canEditExperienceShouldThrowWhenExperienceNotFound() {
        when(experienceRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.usuarioPodeEditarExperiencia(99, 10));
    }

    @Test
    void canRemoveCertificateShouldReturnTrueWhenOwner() {
        FuncionarioCertificado cert = new FuncionarioCertificado(); cert.setFuncionario(employee);
        when(certificateRepository.findById(1)).thenReturn(Optional.of(cert));
        assertTrue(employeeService.usuarioPodeRemoverCertificado(1, 10));
    }

    @Test
    void canRemoveCertificateShouldReturnFalseWhenNotOwner() {
        FuncionarioCertificado cert = new FuncionarioCertificado(); cert.setFuncionario(employee);
        when(certificateRepository.findById(1)).thenReturn(Optional.of(cert));
        assertFalse(employeeService.usuarioPodeRemoverCertificado(1, 99));
    }

    @Test
    void canRemoveCertificateShouldThrowWhenCertificateNotFound() {
        when(certificateRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.usuarioPodeRemoverCertificado(99, 10));
    }

    // removeCertificate
    @Test
    void removeCertificateShouldDeleteWhenExists() {
        when(certificateRepository.existsById(1)).thenReturn(true);
        assertDoesNotThrow(() -> employeeService.removerCertificado(1));
        verify(certificateRepository).deleteById(1);
    }

    @Test
    void removeCertificateShouldThrowWhenNotExists() {
        when(certificateRepository.existsById(99)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> employeeService.removerCertificado(99));
    }

    // linkSkills
    // canAssociate rule: self-edit always passes; gestor can link collaborators in same area;
    // admin can link managers or collaborators in same area; directors cannot be linked by anyone
    private void stubTargetFound() {
        when(employeeRepository.findByIdCompleto(10)).thenReturn(Optional.of(employee));
    }

    private void stubSkillsFound(int count) {
        List<Competencia> skills = new ArrayList<>();
        for (int i = 1; i <= count; i++) { Competencia c = new Competencia(); c.setCodigo(i); skills.add(c); }
        when(skillRepository.findAllById(any())).thenReturn(skills);
    }

    @Test
    void linkSkillsShouldThrowWhenTargetNotFound() {
        login(user(99, 1, "ROLE_ADMIN"));
        when(employeeRepository.findByIdCompleto(10)).thenReturn(Optional.empty());
        var skillIds = List.of(1);
        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldSucceedWhenSelfEdit() {
        login(user(10, 1)); // self-edit
        stubTargetFound(); stubSkillsFound(1);
        when(employeeRepository.save(any())).thenReturn(employee);
        assertDoesNotThrow(() -> employeeService.associarCompetencias(10, List.of(1)));
    }

    @Test
    void linkSkillsShouldThrowWhenSomeSkillsNotFound() {
        login(user(10, 1)); // self-edit passes permission check
        stubTargetFound();
        when(skillRepository.findAllById(any())).thenReturn(List.of()); // 0 found, 2 expected
        var skillIds = List.of(1, 2);
        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldThrowWhenPureRoleUser() {
        // only ROLE_USER, no GESTOR
        login(user(99, 1, "ROLE_USER"));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldThrowWhenRoleUserAndGestorButTargetProfileIsNull() {
        // ROLE_USER+ROLE_GESTOR but target profile is null
        employee.setPerfil(null);
        login(user(99, 1, "ROLE_USER", "ROLE_GESTOR"));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldThrowWhenLoggedAreaIdIsNull() {
        login(user(99, null, "ROLE_GESTOR"));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldThrowWhenTargetAreaIsNull() {
        employee.setArea(null);
        login(user(99, 1, "ROLE_GESTOR"));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldSucceedWhenGestorEditsCollaboratorInSameArea() {
        // same area, target is collaborator
        login(user(99, 1, "ROLE_GESTOR"));
        stubTargetFound(); stubSkillsFound(1);
        when(employeeRepository.save(any())).thenReturn(employee);
        assertDoesNotThrow(() -> employeeService.associarCompetencias(10, List.of(1)));
    }

    @Test
    void linkSkillsShouldThrowWhenGestorTargetsNonCollaborator() {
        // target is a manager, not a collaborator
        profile.setCodigo(2);
        login(user(99, 1, "ROLE_GESTOR"));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldThrowWhenGestorTargetsDifferentArea() {
        // different area
        login(user(99, 2, "ROLE_GESTOR"));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldSucceedWhenAdminTargetsManagerSameArea() {
        // target is manager (profile 2)
        profile.setCodigo(2);
        login(user(99, 1, "ROLE_ADMIN", "ROLE_GESTOR"));
        stubTargetFound(); stubSkillsFound(1);
        when(employeeRepository.save(any())).thenReturn(employee);
        assertDoesNotThrow(() -> employeeService.associarCompetencias(10, List.of(1)));
    }

    @Test
    void linkSkillsShouldSucceedWhenAdminTargetsCollaboratorSameArea() {
        // target is collaborator (profile 3)
        login(user(99, 1, "ROLE_ADMIN", "ROLE_GESTOR"));
        stubTargetFound(); stubSkillsFound(1);
        when(employeeRepository.save(any())).thenReturn(employee);
        assertDoesNotThrow(() -> employeeService.associarCompetencias(10, List.of(1)));
    }

    @Test
    void linkSkillsShouldThrowWhenAdminTargetsDirector() {
        // target is director (profile 1)
        profile.setCodigo(1);
        login(user(99, 1, "ROLE_ADMIN", "ROLE_GESTOR"));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldThrowWhenAdminIsInDifferentArea() {
        // different area
        login(user(99, 2, "ROLE_ADMIN", "ROLE_GESTOR"));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }

    @Test
    void linkSkillsShouldThrowWhenUserHasNoRole() {
        // no role assigned
        login(user(99, 1));
        stubTargetFound();
        var skillIds = List.of(1);
        assertThrows(UnauthorizedActionException.class,
                () -> employeeService.associarCompetencias(10, skillIds));
    }
}
