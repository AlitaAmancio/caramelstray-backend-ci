package br.com.AllTallent.dto;

import java.util.List;
import java.util.stream.Collectors;

import br.com.AllTallent.model.Funcionario;

public record FuncionarioCompetenciasResponseDTO(
    List<CompetenciaDTO> competencias
) {
    public FuncionarioCompetenciasResponseDTO(Funcionario funcionario) {
        this(
            funcionario.getCompetencias() != null ?
                funcionario.getCompetencias().stream().map(CompetenciaDTO::new).collect(Collectors.toList()) :
                List.of()
        );
    }
}
