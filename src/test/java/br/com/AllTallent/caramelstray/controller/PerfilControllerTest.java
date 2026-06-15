package br.com.AllTallent.caramelstray.controller;

import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.PerfilController;
import br.com.AllTallent.dto.PerfilDTO;
import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.repository.PerfilRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PerfilController.class)
class PerfilControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PerfilRepository perfilRepository;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private FuncionarioRepository funcionarioRepository;

    @Test
    @WithMockUser
    void createPerfilShouldReturn201WhenSaved() throws Exception {
        Perfil saved = new Perfil();
        saved.setCodigo(1);
        saved.setNome("Developer");
        saved.setDescricao("Software developer");

        when(perfilRepository.save(any(Perfil.class))).thenReturn(saved);

        PerfilDTO request = new PerfilDTO(null, "Developer", "Software developer");

        mockMvc.perform(post("/api/perfil")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Developer"));
    }

    @Test
    @WithMockUser
    void getAllPerfisShouldReturnListOf200() throws Exception {
        Perfil p1 = new Perfil(); p1.setCodigo(1); p1.setNome("Developer"); p1.setDescricao("d1");
        Perfil p2 = new Perfil(); p2.setCodigo(2); p2.setNome("Manager"); p2.setDescricao("d2");

        when(perfilRepository.findAll()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
