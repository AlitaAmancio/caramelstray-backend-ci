package br.com.AllTallent.caramelstray.controller;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.FuncionarioController;
import br.com.AllTallent.dto.CertificadoDTO;
import br.com.AllTallent.dto.CertificadoRequestDTO;
import br.com.AllTallent.dto.ExperienciaDTO;
import br.com.AllTallent.dto.ExperienciaRequestDTO;
import br.com.AllTallent.dto.FuncionarioCompetenciaUpdateDTO;
import br.com.AllTallent.dto.FuncionarioCompetenciasResponseDTO;
import br.com.AllTallent.dto.FuncionarioExperienciasResponseDTO;
import br.com.AllTallent.dto.FuncionarioPerfilDTO;
import br.com.AllTallent.dto.FuncionarioRequestDTO;
import br.com.AllTallent.dto.FuncionarioResponseDTO;
import br.com.AllTallent.exception.ResourceNotFoundException;
import br.com.AllTallent.exception.UnauthorizedActionException;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.service.FuncionarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FuncionarioController.class)
class FuncionarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private FuncionarioService funcionarioService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private FuncionarioRepository funcionarioRepository;

    private Authentication userAuth(int codigo) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        CustomUserDetails ud = new CustomUserDetails(f);
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    private Authentication adminAuth(int codigo) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        Perfil p = new Perfil();
        p.setCodigo(1);
        f.setPerfil(p);
        CustomUserDetails ud = new CustomUserDetails(f);
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    private FuncionarioResponseDTO employeeDTO(int id) {
        Funcionario f = new Funcionario();
        f.setCodigo(id);
        f.setNomeCompleto("John Smith");
        return new FuncionarioResponseDTO(f);
    }

    private FuncionarioRequestDTO updateRequest() {
        return new FuncionarioRequestDTO("John Smith", "john@test.com", "000.000.000-00",
                "11999999999", "password", 1, 1, null, "Engineer", "SP", "Summary");
    }

    private ExperienciaRequestDTO experienceRequest() {
        return new ExperienciaRequestDTO("Developer", "Tech Corp", LocalDate.of(2023, 1, 1), null, "Description");
    }

    @Test
    @WithMockUser
    void listAllShouldReturn200() throws Exception {
        when(funcionarioService.listarTodos(null)).thenReturn(List.of(employeeDTO(1)));

        mockMvc.perform(get("/api/funcionario"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByIdShouldReturn200AsAdmin() throws Exception {
        when(funcionarioService.buscarPorId(1)).thenReturn(employeeDTO(1));

        mockMvc.perform(get("/api/funcionario/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEmployeeShouldReturn201() throws Exception {
        when(funcionarioService.criar(any())).thenReturn(employeeDTO(1));

        mockMvc.perform(post("/api/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void updateEmployeeShouldReturn200WhenPrincipalMatchesId() throws Exception {
        when(funcionarioService.atualizar(eq(1), any())).thenReturn(employeeDTO(1));

        mockMvc.perform(put("/api/funcionario/1")
                        .with(csrf())
                        .with(authentication(userAuth(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteEmployeeShouldReturn204() throws Exception {
        doNothing().when(funcionarioService).deletar(1);

        mockMvc.perform(delete("/api/funcionario/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProfileByIdShouldReturn200() throws Exception {
        when(funcionarioService.buscarPerfilPorId(1)).thenReturn(mock(FuncionarioPerfilDTO.class));

        mockMvc.perform(get("/api/funcionario/1/perfil"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addCertificateShouldReturn201() throws Exception {
        when(funcionarioService.adicionarCertificado(eq(1), any())).thenReturn(new CertificadoDTO(1, "AWS"));

        CertificadoRequestDTO req = new CertificadoRequestDTO("AWS");

        mockMvc.perform(post("/api/funcionario/1/certificados")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeCertificateShouldReturn204() throws Exception {
        doNothing().when(funcionarioService).removerCertificado(1);

        mockMvc.perform(delete("/api/funcionario/certificados/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void updateCompetenciasShouldReturn204OnSuccess() throws Exception {
        doNothing().when(funcionarioService).associarCompetencias(eq(1), any());

        FuncionarioCompetenciaUpdateDTO req = new FuncionarioCompetenciaUpdateDTO(List.of(1, 2, 3));

        mockMvc.perform(put("/api/funcionario/1/competencias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void updateCompetenciasShouldReturn404OnResourceNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Not found")).when(funcionarioService)
                .associarCompetencias(eq(1), any());

        FuncionarioCompetenciaUpdateDTO req = new FuncionarioCompetenciaUpdateDTO(List.of(99));

        mockMvc.perform(put("/api/funcionario/1/competencias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateCompetenciasShouldReturn403OnUnauthorized() throws Exception {
        doThrow(new UnauthorizedActionException("Forbidden")).when(funcionarioService)
                .associarCompetencias(eq(1), any());

        FuncionarioCompetenciaUpdateDTO req = new FuncionarioCompetenciaUpdateDTO(List.of(1));

        mockMvc.perform(put("/api/funcionario/1/competencias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listCompetenciasShouldReturn200() throws Exception {
        Funcionario f = new Funcionario();
        f.setCodigo(1);
        when(funcionarioService.buscarFuncionarioCompleto(1)).thenReturn(f);

        mockMvc.perform(get("/api/funcionario/1/competencias"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listExperienciasShouldReturn200() throws Exception {
        when(funcionarioService.listarExperienciasPorFuncionario(1))
                .thenReturn(mock(FuncionarioExperienciasResponseDTO.class));

        mockMvc.perform(get("/api/funcionario/1/experiencias"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addExperienciaShouldReturn201() throws Exception {
        when(funcionarioService.adicionarExperiencia(eq(1), any()))
                .thenReturn(new ExperienciaDTO(1, "Developer", "Tech Corp", "Desc",
                        LocalDate.of(2023, 1, 1), null));

        mockMvc.perform(post("/api/funcionario/1/experiencias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(experienceRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateExperienciaShouldReturn200() throws Exception {
        when(funcionarioService.atualizarExperiencia(eq(1), any()))
                .thenReturn(new ExperienciaDTO(1, "Senior Developer", "Tech Corp", "Desc",
                        LocalDate.of(2023, 1, 1), null));

        mockMvc.perform(put("/api/funcionario/experiencias/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(experienceRequest())))
                .andExpect(status().isOk());
    }
}
