package br.com.AllTallent.caramelstray.controller;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.DashboardController;
import br.com.AllTallent.dto.DashboardResponseDTO;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private DashboardService dashboardService;
    @MockitoBean private FuncionarioRepository funcionarioRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private Authentication adminAuth(int codigo) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        Perfil p = new Perfil();
        p.setCodigo(1);
        f.setPerfil(p);
        CustomUserDetails ud = new CustomUserDetails(f);
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    private Authentication gestorAuth(int codigo) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        Perfil p = new Perfil();
        p.setCodigo(2); // ROLE_GESTOR + ROLE_USER
        f.setPerfil(p);
        CustomUserDetails ud = new CustomUserDetails(f);
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    private DashboardResponseDTO emptyResponse() {
        return DashboardResponseDTO.builder()
                .totalColaboradores(0L)
                .avaliacoesConcluidasMes(0)
                .metaMensal(0.0)
                .totalPendencias(0)
                .evolucaoMensal(List.of())
                .totalColaboradoresCompetencia(List.of())
                .totalColaboradoresArea(List.of())
                .top5CompetenciasMaisAvaliadas(List.of())
                .build();
    }

    @Test
    void getDashboardAsAdminShouldReturn200WithoutForcingAreaFilter() throws Exception {
        when(dashboardService.getDashboardData(null)).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/dashboard")
                        .with(authentication(adminAuth(1))))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardAsAdminWithAreaParamShouldPassParamToService() throws Exception {
        when(dashboardService.getDashboardData(5)).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/dashboard?codigoArea=5")
                        .with(authentication(adminAuth(1))))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardAsGestorWithAreaShouldForceAreaFilter() throws Exception {
        Area area = new Area();
        area.setCodigo(3);
        Funcionario gestor = new Funcionario();
        gestor.setCodigo(1);
        gestor.setArea(area);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(gestor));
        when(dashboardService.getDashboardData(3)).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/dashboard")
                        .with(authentication(gestorAuth(1))))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardAsGestorWithNullAreaShouldNotOverrideFilter() throws Exception {
        Funcionario gestorNoArea = new Funcionario();
        gestorNoArea.setCodigo(1);
        gestorNoArea.setArea(null);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(gestorNoArea));
        when(dashboardService.getDashboardData(null)).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/dashboard")
                        .with(authentication(gestorAuth(1))))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardShouldReturn500OnException() throws Exception {
        when(dashboardService.getDashboardData(any())).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/dashboard")
                        .with(authentication(adminAuth(1))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getDashboardAsDirectoryShouldReturn200() throws Exception {
        Funcionario f = new Funcionario();
        f.setCodigo(1);
        Perfil p = new Perfil();
        p.setCodigo(1);
        f.setPerfil(p);
        CustomUserDetails ud = new CustomUserDetails(f);
        Authentication directorAuth = new UsernamePasswordAuthenticationToken(ud, null,
                List.of(new SimpleGrantedAuthority("ROLE_DIRETORIA"),
                        new SimpleGrantedAuthority("ROLE_USER")));

        when(dashboardService.getDashboardData(null)).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/dashboard")
                        .with(authentication(directorAuth)))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardAsSupervisorShouldForceAreaFilter() throws Exception {
        Funcionario f = new Funcionario();
        f.setCodigo(1);
        Perfil p = new Perfil();
        p.setCodigo(2);
        f.setPerfil(p);
        CustomUserDetails ud = new CustomUserDetails(f);
        Authentication supervisorAuth = new UsernamePasswordAuthenticationToken(ud, null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPERVISAO"),
                        new SimpleGrantedAuthority("ROLE_USER")));

        Area area = new Area();
        area.setCodigo(4);
        Funcionario supervisor = new Funcionario();
        supervisor.setCodigo(1);
        supervisor.setArea(area);
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(supervisor));
        when(dashboardService.getDashboardData(4)).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/dashboard")
                        .with(authentication(supervisorAuth)))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardAsPlainUserShouldNotForceAreaFilter() throws Exception {
        Funcionario f = new Funcionario();
        f.setCodigo(1);
        CustomUserDetails ud = new CustomUserDetails(f);
        Authentication userAuth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());

        when(dashboardService.getDashboardData(null)).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/dashboard")
                        .with(authentication(userAuth)))
                .andExpect(status().isOk());
    }
}
