package br.com.AllTallent.controller;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.dto.DashboardResponseDTO;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final FuncionarioRepository funcionarioRepository;

    /**
     * Retorna dados do Dashboard.
     * - Se for GESTOR: Retorna apenas dados da sua equipe/área.
     * - Se for ADMIN: Pode ver tudo ou filtrar por ?codigoArea=X
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> getDashboardData(
            @RequestParam(required = false) Integer codigoArea,
            Authentication authentication) {

        log.info("Dashboard endpoint invoked");

        try {
            // --- LÓGICA DE PERMISSÃO ---
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer usuarioLogadoId = userDetails.getCodigo();

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DIRETORIA"));
            
            boolean isGestor = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR") || a.getAuthority().equals("ROLE_SUPERVISAO"));

            Integer filtroAreaId = codigoArea;

            // Se for Gestor (e não Admin), FORÇA o filtro para a área dele.
            if (isGestor && !isAdmin) {
                Funcionario gestor = funcionarioRepository.findById(usuarioLogadoId)
                        .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
                
                if (gestor.getArea() != null) {
                    filtroAreaId = gestor.getArea().getCodigo();
                    log.info("Dashboard area filter applied for gestor: {}", filtroAreaId);
                }
            }

            // --- CHAMADA AO SERVICE ---
            DashboardResponseDTO data = dashboardService.getDashboardData(filtroAreaId);
            
            log.info("Dashboard data retrieved successfully");

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            // --- CAPTURA DO ERRO ---
            log.error("Error while building dashboard response", e);
            return ResponseEntity.internalServerError().body("Erro interno no servidor: " + e.getMessage());
        }
    }
}