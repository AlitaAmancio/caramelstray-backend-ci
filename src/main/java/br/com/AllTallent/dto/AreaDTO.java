package br.com.AllTallent.dto;

import br.com.AllTallent.model.Area;

public record AreaDTO(Integer codigo, String nome, String descricao) {

    public AreaDTO(Area area) {
        this(area.getCodigo(), area.getNome(), area.getDescricao());
    }

    public Area toEntity() {
        Area area = new Area();
        area.setCodigo(codigo);
        area.setNome(nome);
        area.setDescricao(descricao);
        return area;
    }
}