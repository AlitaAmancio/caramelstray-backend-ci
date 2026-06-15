package br.com.AllTallent.caramelstray.controller;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.AvaliacaoController;
import br.com.AllTallent.dto.AvaliacaoDetalhadaDTO;
import br.com.AllTallent.dto.AvaliacaoFuncionarioResponseDTO;
import br.com.AllTallent.dto.AvaliacaoParaResponderDTO;
import br.com.AllTallent.dto.AvaliacaoRequestDTO;
import br.com.AllTallent.dto.AvaliacaoResponseDTO;
import br.com.AllTallent.dto.RespostaColaboradorRequestDTO;
import br.com.AllTallent.dto.RespostaColaboradorResponseDTO;
import br.com.AllTallent.dto.RevisaoDetalhadaDTO;
import br.com.AllTallent.dto.RevisaoSupervisorRequestDTO;
import br.com.AllTallent.exception.ResourceNotFoundException;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.service.AvaliacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
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
import java.time.ZoneId;
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

@WebMvcTest(controllers = AvaliacaoController.class)
class AvaliacaoControllerTest {

    private static final String AVALIACOES_URL = "/api/avaliacoes";
    private static final String RESPOSTAS_URL = "/api/avaliacoes/respostas";
    private static final String INSTANCIAS_URL = "/api/avaliacoes/instancias";
    private static final String NOT_FOUND_MSG = "Not found";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AvaliacaoService avaliacaoService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private FuncionarioRepository funcionarioRepository;

    private Authentication userAuth(int codigo) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        CustomUserDetails ud = new CustomUserDetails(f);
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    private AvaliacaoResponseDTO avaliacaoResponse() {
        return new AvaliacaoResponseDTO(1, "Test Evaluation", "PENDENTE", null, null, "System");
    }

    private AvaliacaoRequestDTO avaliacaoRequest() {
        return new AvaliacaoRequestDTO("Test Evaluation",
                LocalDate.now(ZoneId.of("UTC")).plusDays(30), List.of(1), List.of(1L));
    }

    private RespostaColaboradorRequestDTO respostaRequest() {
        return new RespostaColaboradorRequestDTO(1L, 1L, "My answer", null);
    }

    private RevisaoSupervisorRequestDTO revisaoRequest() {
        return new RevisaoSupervisorRequestDTO("Good performance", "Well done", "APROVADO");
    }

    //criarAvaliacao
    @Test
    @WithMockUser(roles = "ADMIN")
    void createAvaliacaoShouldReturn201OnSuccess() throws Exception {
        when(avaliacaoService.criarAvaliacaoCompleta(any())).thenReturn(avaliacaoResponse());

        mockMvc.perform(post(AVALIACOES_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(avaliacaoRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAvaliacaoShouldReturn400OnEntityNotFound() throws Exception {
        when(avaliacaoService.criarAvaliacaoCompleta(any())).thenThrow(new EntityNotFoundException(NOT_FOUND_MSG));

        mockMvc.perform(post(AVALIACOES_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(avaliacaoRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAvaliacaoShouldReturn500OnGenericException() throws Exception {
        when(avaliacaoService.criarAvaliacaoCompleta(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post(AVALIACOES_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(avaliacaoRequest())))
                .andExpect(status().isInternalServerError());
    }

    //listarTodasAvaliacoes
    @Test
    @WithMockUser(roles = "ADMIN")
    void listAllAvaliacoesShouldReturn200() throws Exception {
        when(avaliacaoService.listarTodasAvaliacoes()).thenReturn(List.of(avaliacaoResponse()));

        mockMvc.perform(get(AVALIACOES_URL))
                .andExpect(status().isOk());
    }

    //buscarAvaliacaoDetalhada
    @Test
    @WithMockUser(roles = "ADMIN")
    void findDetailedAvaliacaoShouldReturn200() throws Exception {
        when(avaliacaoService.buscarAvaliacaoDetalhada(1)).thenReturn(mock(AvaliacaoDetalhadaDTO.class));

        mockMvc.perform(get(AVALIACOES_URL + "/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findDetailedAvaliacaoShouldReturn404OnNotFound() throws Exception {
        when(avaliacaoService.buscarAvaliacaoDetalhada(99))
                .thenThrow(new ResourceNotFoundException(NOT_FOUND_MSG));

        mockMvc.perform(get(AVALIACOES_URL + "/99"))
                .andExpect(status().isNotFound());
    }

    //buscarInstanciasPorAvaliacao
    @Test
    @WithMockUser(roles = "ADMIN")
    void findInstancesByAvaliacaoShouldReturn200() throws Exception {
        when(avaliacaoService.buscarInstanciasPorAvaliacao(1))
                .thenReturn(List.of(mock(AvaliacaoFuncionarioResponseDTO.class)));

        mockMvc.perform(get(AVALIACOES_URL + "/1/instancias"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findInstancesByAvaliacaoShouldReturn404OnNotFound() throws Exception {
        when(avaliacaoService.buscarInstanciasPorAvaliacao(99))
                .thenThrow(new EntityNotFoundException(NOT_FOUND_MSG));

        mockMvc.perform(get(AVALIACOES_URL + "/99/instancias"))
                .andExpect(status().isNotFound());
    }

    //salvarResposta
    @Test
    @WithMockUser
    void saveRespostaShouldReturn200OnSuccess() throws Exception {
        when(avaliacaoService.salvarOuAtualizarResposta(any()))
                .thenReturn(new RespostaColaboradorResponseDTO(1L, 1L, 1L, "My answer", null));

        mockMvc.perform(post(RESPOSTAS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respostaRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void saveRespostaShouldReturn400OnEntityNotFound() throws Exception {
        when(avaliacaoService.salvarOuAtualizarResposta(any()))
                .thenThrow(new EntityNotFoundException("Instance not found"));

        mockMvc.perform(post(RESPOSTAS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respostaRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void saveRespostaShouldReturn400OnIllegalArgument() throws Exception {
        when(avaliacaoService.salvarOuAtualizarResposta(any()))
                .thenThrow(new IllegalArgumentException("Invalid data"));

        mockMvc.perform(post(RESPOSTAS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respostaRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void saveRespostaShouldReturn500OnGenericException() throws Exception {
        when(avaliacaoService.salvarOuAtualizarResposta(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post(RESPOSTAS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respostaRequest())))
                .andExpect(status().isInternalServerError());
    }

    //buscarRespostasPorInstancia
    @Test
    @WithMockUser(roles = "ADMIN")
    void findRespostasByInstanciaShouldReturn200() throws Exception {
        when(avaliacaoService.buscarRespostasPorInstancia(1L))
                .thenReturn(List.of(new RespostaColaboradorResponseDTO(1L, 1L, 1L, "text", null)));

        mockMvc.perform(get(INSTANCIAS_URL + "/1/respostas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findRespostasByInstanciaShouldReturn404OnNotFound() throws Exception {
        when(avaliacaoService.buscarRespostasPorInstancia(99L))
                .thenThrow(new EntityNotFoundException(NOT_FOUND_MSG));

        mockMvc.perform(get(INSTANCIAS_URL + "/99/respostas"))
                .andExpect(status().isNotFound());
    }

    //getDadosParaRevisao
    @Test
    @WithMockUser(roles = "ADMIN")
    void getRevisaoDataShouldReturn200() throws Exception {
        when(avaliacaoService.buscarDadosRevisao(1L))
                .thenReturn(List.of(new RevisaoDetalhadaDTO()));

        mockMvc.perform(get(AVALIACOES_URL + "/revisao/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRevisaoDataShouldReturn404OnNotFound() throws Exception {
        when(avaliacaoService.buscarDadosRevisao(99L))
                .thenThrow(new EntityNotFoundException(NOT_FOUND_MSG));

        mockMvc.perform(get(AVALIACOES_URL + "/revisao/99"))
                .andExpect(status().isNotFound());
    }

    //salvarRevisaoSupervisor
    @Test
    @WithMockUser(roles = "ADMIN")
    void saveRevisaoShouldReturn200OnSuccess() throws Exception {
        when(avaliacaoService.salvarRevisaoSupervisor(eq(1L), any()))
                .thenReturn(mock(AvaliacaoFuncionarioResponseDTO.class));

        mockMvc.perform(put(INSTANCIAS_URL + "/1/revisar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revisaoRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveRevisaoShouldReturn404OnEntityNotFound() throws Exception {
        when(avaliacaoService.salvarRevisaoSupervisor(eq(99L), any()))
                .thenThrow(new EntityNotFoundException(NOT_FOUND_MSG));

        mockMvc.perform(put(INSTANCIAS_URL + "/99/revisar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revisaoRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveRevisaoShouldReturn500OnGenericException() throws Exception {
        when(avaliacaoService.salvarRevisaoSupervisor(eq(1L), any()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(put(INSTANCIAS_URL + "/1/revisar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revisaoRequest())))
                .andExpect(status().isInternalServerError());
    }

    //buscarAvaliacoesPendentes
    @Test
    void findPendentesShouldReturn200WhenPrincipalMatchesId() throws Exception {
        when(avaliacaoService.buscarPendentesPorFuncionario(1))
                .thenReturn(List.of(mock(AvaliacaoFuncionarioResponseDTO.class)));

        mockMvc.perform(get(AVALIACOES_URL + "/pendentes/1")
                        .with(authentication(userAuth(1))))
                .andExpect(status().isOk());
    }

    //buscarAvaliacaoParaResponder
    @Test
    @WithMockUser
    void findParaResponderShouldReturn200() throws Exception {
        when(avaliacaoService.buscarParaResponder(1L))
                .thenReturn(mock(AvaliacaoParaResponderDTO.class));

        mockMvc.perform(get(INSTANCIAS_URL + "/1/responder"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void findParaResponderShouldReturn404OnNotFound() throws Exception {
        when(avaliacaoService.buscarParaResponder(99L))
                .thenThrow(new EntityNotFoundException(NOT_FOUND_MSG));

        mockMvc.perform(get(INSTANCIAS_URL + "/99/responder"))
                .andExpect(status().isNotFound());
    }

    //finalizarAvaliacaoColaborador
    @Test
    @WithMockUser
    void finalizeAvaliacaoShouldReturn204OnSuccess() throws Exception {
        doNothing().when(avaliacaoService).finalizarPeloColaborador(1L);

        mockMvc.perform(put(INSTANCIAS_URL + "/1/finalizar").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void finalizeAvaliacaoShouldReturn404OnEntityNotFound() throws Exception {
        doThrow(new EntityNotFoundException(NOT_FOUND_MSG)).when(avaliacaoService)
                .finalizarPeloColaborador(99L);

        mockMvc.perform(put(INSTANCIAS_URL + "/99/finalizar").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void finalizeAvaliacaoShouldReturn409OnIllegalState() throws Exception {
        doThrow(new IllegalStateException("Already finalized")).when(avaliacaoService)
                .finalizarPeloColaborador(1L);

        mockMvc.perform(put(INSTANCIAS_URL + "/1/finalizar").with(csrf()))
                .andExpect(status().isConflict());
    }
}
