package br.com.AllTallent.caramelstray.service;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.AllTallent.dto.CadastroRequestDTO;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.repository.AreaRepository;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.repository.PerfilRepository;
import br.com.AllTallent.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private FuncionarioRepository employeeRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private PerfilRepository profileRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private CadastroRequestDTO request;
    private Area area;
    private Perfil profile;
    private Funcionario manager;

    @BeforeEach
    void setUp() {
        request = new CadastroRequestDTO();
        request.setNomeCompleto("John Smith");
        request.setEmail("john.smith@alltallent.com");
        request.setSenha("password123");
        request.setTelefone("123456789");
        request.setResumo("Java Developer");
        request.setCodigoArea(1);
        request.setCodigoPerfil(2);
        request.setCpf("123.456.789-00");
        request.setLocalizacao("Sao Paulo, SP");
        request.setTituloProfissional("Software Engineer");
        request.setIdCracha("CR-9988");
        request.setDataAdmissao(LocalDate.now());
        request.setCodigoGestor(10);

        area = new Area();
        area.setNome("Technology");

        profile = new Perfil();
        profile.setNome("Developer");

        manager = new Funcionario();
        manager.setNomeCompleto("Technical Manager");
    }

    @Test
    void shouldRegisterEmployeeSuccessfully() {
        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(areaRepository.findById(request.getCodigoArea())).thenReturn(Optional.of(area));
        when(profileRepository.findById(request.getCodigoPerfil())).thenReturn(Optional.of(profile));
        when(passwordEncoder.encode(request.getSenha())).thenReturn("encryptedPassword");
        when(employeeRepository.findById(request.getCodigoGestor())).thenReturn(Optional.of(manager));
        when(employeeRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        Funcionario result = authService.register(request);

        assertNotNull(result);
        assertEquals("John Smith", result.getNomeCompleto());
        assertEquals("john.smith@alltallent.com", result.getEmail());
        assertEquals("encryptedPassword", result.getSenhaHash());
        assertEquals(area, result.getArea());
        assertEquals(profile, result.getPerfil());
        assertEquals(manager, result.getGestor());
        assertNotNull(result.getDataCadastro());
        verify(employeeRepository).save(any(Funcionario.class));
    }

    @Test
    void shouldRegisterSuccessfullyWhenManagerIsNull() {
        request.setCodigoGestor(null);

        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(areaRepository.findById(request.getCodigoArea())).thenReturn(Optional.of(area));
        when(profileRepository.findById(request.getCodigoPerfil())).thenReturn(Optional.of(profile));
        when(passwordEncoder.encode(request.getSenha())).thenReturn("encryptedPassword");
        when(employeeRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        Funcionario result = authService.register(request);

        assertNull(result.getGestor());
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void shouldThrowWhenEmailAlreadyInUse() {
        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new Funcionario()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.register(request));

        assertEquals("Erro: Email já está em uso!", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAreaNotFound() {
        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(areaRepository.findById(request.getCodigoArea())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        assertEquals("Erro: Área (Departamento) não encontrada.", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenProfileNotFound() {
        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(areaRepository.findById(request.getCodigoArea())).thenReturn(Optional.of(area));
        when(profileRepository.findById(request.getCodigoPerfil())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        assertEquals("Erro: Perfil (Cargo) não encontrado.", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenManagerNotFound() {
        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(areaRepository.findById(request.getCodigoArea())).thenReturn(Optional.of(area));
        when(profileRepository.findById(request.getCodigoPerfil())).thenReturn(Optional.of(profile));
        when(passwordEncoder.encode(request.getSenha())).thenReturn("encryptedPassword");
        when(employeeRepository.findById(request.getCodigoGestor())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        assertEquals("Erro: Gestor informado não encontrado.", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }
}
