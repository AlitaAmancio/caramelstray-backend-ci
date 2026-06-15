package br.com.AllTallent.caramelstray.controller;

import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.AreaController;
import br.com.AllTallent.dto.AreaDTO;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.repository.AreaRepository;
import br.com.AllTallent.repository.FuncionarioRepository;
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

@WebMvcTest(controllers = AreaController.class)
class AreaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AreaRepository areaRepository;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private FuncionarioRepository funcionarioRepository;

    @Test
    @WithMockUser
    void createAreaShouldReturn201WhenSaved() throws Exception {
        Area saved = new Area();
        saved.setCodigo(1);
        saved.setNome("Technology");
        saved.setDescricao("Tech dept");

        when(areaRepository.save(any(Area.class))).thenReturn(saved);

        AreaDTO request = new AreaDTO(null, "Technology", "Tech dept");

        mockMvc.perform(post("/api/area")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Technology"));
    }

    @Test
    @WithMockUser
    void getAllAreasShouldReturnListOf200() throws Exception {
        Area a1 = new Area(); a1.setCodigo(1); a1.setNome("Tech"); a1.setDescricao("d1");
        Area a2 = new Area(); a2.setCodigo(2); a2.setNome("HR"); a2.setDescricao("d2");

        when(areaRepository.findAll()).thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/api/area"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
