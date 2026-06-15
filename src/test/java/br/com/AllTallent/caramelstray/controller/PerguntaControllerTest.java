package br.com.AllTallent.caramelstray.controller;

import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.PerguntaController;
import br.com.AllTallent.dto.PerguntaRequestDTO;
import br.com.AllTallent.dto.PerguntaResponseDTO;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.service.PerguntaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PerguntaController.class)
class PerguntaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PerguntaService perguntaService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private FuncionarioRepository funcionarioRepository;

    private PerguntaResponseDTO responseDTO() {
        return new PerguntaResponseDTO(1L, "What is Java?", 1, "Java");
    }

    private PerguntaRequestDTO requestDTO() {
        return new PerguntaRequestDTO("What is Java?", 1, "ABERTA", null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createQuestionShouldReturn201OnSuccess() throws Exception {
        when(perguntaService.criarPergunta(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/api/perguntas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createQuestionShouldReturn400OnEntityNotFound() throws Exception {
        when(perguntaService.criarPergunta(any())).thenThrow(new EntityNotFoundException("Skill not found"));

        mockMvc.perform(post("/api/perguntas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAllQuestionsShouldReturn200WithList() throws Exception {
        when(perguntaService.listarTodas()).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/api/perguntas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByIdShouldReturn200WhenFound() throws Exception {
        when(perguntaService.buscarPorId(1L)).thenReturn(responseDTO());

        mockMvc.perform(get("/api/perguntas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pergunta").value("What is Java?"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByIdShouldReturn404WhenNotFound() throws Exception {
        when(perguntaService.buscarPorId(99L)).thenThrow(new EntityNotFoundException("Not found"));

        mockMvc.perform(get("/api/perguntas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteShouldReturn204WhenExists() throws Exception {
        doNothing().when(perguntaService).deletarPergunta(1L);

        mockMvc.perform(delete("/api/perguntas/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteShouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Not found")).when(perguntaService).deletarPergunta(99L);

        mockMvc.perform(delete("/api/perguntas/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
