package br.com.AllTallent.caramelstray.controller;

import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.CompetenciaController;
import br.com.AllTallent.dto.CompetenciaDTO;
import br.com.AllTallent.model.Competencia;
import br.com.AllTallent.repository.CompetenciaRepository;
import br.com.AllTallent.repository.FuncionarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompetenciaController.class)
class CompetenciaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CompetenciaRepository competenciaRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private FuncionarioRepository funcionarioRepository;

    private Competencia entity(int id, String nome) {
        Competencia c = new Competencia();
        c.setCodigo(id);
        c.setNome(nome);
        c.setCategoria("Technical");
        return c;
    }

    @Test
    @WithMockUser
    void listAllShouldReturn200WithList() throws Exception {
        when(competenciaRepository.findAll()).thenReturn(List.of(entity(1, "Java"), entity(2, "Python")));

        mockMvc.perform(get("/api/competencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void findByIdShouldReturn200WhenFound() throws Exception {
        when(competenciaRepository.findById(1)).thenReturn(Optional.of(entity(1, "Java")));

        mockMvc.perform(get("/api/competencia/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Java"));
    }

    @Test
    @WithMockUser
    void findByIdShouldReturn404WhenNotFound() throws Exception {
        when(competenciaRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/competencia/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void createShouldReturn201WhenNameIsUnique() throws Exception {
        Competencia saved = entity(1, "Java");
        when(competenciaRepository.existsByNomeIgnoreCase("Java")).thenReturn(false);
        when(competenciaRepository.save(any(Competencia.class))).thenReturn(saved);

        CompetenciaDTO request = new CompetenciaDTO(null, "Java", "Technical");

        mockMvc.perform(post("/api/competencia")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Java"));
    }

    @Test
    @WithMockUser
    void createShouldReturn400WhenNameIsDuplicate() throws Exception {
        when(competenciaRepository.existsByNomeIgnoreCase("Java")).thenReturn(true);

        CompetenciaDTO request = new CompetenciaDTO(null, "Java", "Technical");

        mockMvc.perform(post("/api/competencia")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void updateShouldReturn200WhenFound() throws Exception {
        Competencia existing = entity(1, "Java");
        Competencia updated = entity(1, "Java Advanced");
        when(competenciaRepository.findById(1)).thenReturn(Optional.of(existing));
        when(competenciaRepository.save(any(Competencia.class))).thenReturn(updated);

        CompetenciaDTO request = new CompetenciaDTO(null, "Java Advanced", "Technical");

        mockMvc.perform(put("/api/competencia/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Java Advanced"));
    }

    @Test
    @WithMockUser
    void updateShouldReturn404WhenNotFound() throws Exception {
        when(competenciaRepository.findById(99)).thenReturn(Optional.empty());

        CompetenciaDTO request = new CompetenciaDTO(null, "Java", "Technical");

        mockMvc.perform(put("/api/competencia/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteShouldReturn204WhenExists() throws Exception {
        when(competenciaRepository.existsById(1)).thenReturn(true);

        mockMvc.perform(delete("/api/competencia/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteShouldReturn404WhenNotFound() throws Exception {
        when(competenciaRepository.existsById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/competencia/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
