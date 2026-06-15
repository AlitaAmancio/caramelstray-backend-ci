package br.com.AllTallent.caramelstray.controller;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.AuthController;
import br.com.AllTallent.dto.CadastroRequestDTO;
import br.com.AllTallent.dto.FuncionarioResponseDTO;
import br.com.AllTallent.dto.LoginRequestDTO;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.service.AuthService;
import br.com.AllTallent.service.FuncionarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private AuthenticationProvider authenticationProvider;
    @MockBean private FuncionarioRepository funcionarioRepository;
    @MockBean private JwtService jwtService;
    @MockBean private FuncionarioService funcionarioService;
    @MockBean private AuthService authService;
    @MockBean private UserDetailsService userDetailsService;

    private Authentication buildAuth(int codigo) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        Perfil p = new Perfil();
        p.setCodigo(1);
        f.setPerfil(p);
        CustomUserDetails ud = new CustomUserDetails(f);
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    private CadastroRequestDTO registerRequest() {
        CadastroRequestDTO dto = new CadastroRequestDTO();
        dto.setNomeCompleto("John Smith");
        dto.setEmail("john@test.com");
        dto.setSenha("password123");
        dto.setIdCracha("CR-001");
        dto.setCpf("000.000.000-00");
        dto.setCodigoArea(1);
        dto.setCodigoPerfil(2);
        return dto;
    }

    @Test
    @WithMockUser
    void loginShouldReturn200WithTokenOnSuccess() throws Exception {
        Funcionario employee = new Funcionario();
        employee.setCodigo(1);
        employee.setNomeCompleto("John Smith");

        UserDetails ud = mock(UserDetails.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(ud);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(funcionarioRepository.findByEmail("john@test.com")).thenReturn(Optional.of(employee));
        when(jwtService.generateToken(ud)).thenReturn("jwt-token-abc");

        LoginRequestDTO request = new LoginRequestDTO("john@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-abc"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithMockUser
    void registerShouldReturn201OnSuccess() throws Exception {
        Funcionario saved = new Funcionario();
        saved.setCodigo(5);
        when(authService.register(any())).thenReturn(saved);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void registerShouldReturn400OnRuntimeException() throws Exception {
        when(authService.register(any())).thenThrow(new RuntimeException("Email em uso"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMeShouldReturn200WithProfile() throws Exception {
        Funcionario f = new Funcionario();
        f.setCodigo(1);
        f.setNomeCompleto("John Smith");
        f.setArea(new Area());
        FuncionarioResponseDTO dto = new FuncionarioResponseDTO(f);
        when(funcionarioService.buscarPorId(1)).thenReturn(dto);

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(buildAuth(1))))
                .andExpect(status().isOk());
    }
}
