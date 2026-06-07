package br.com.AllTallent.dto;

import br.com.AllTallent.model.Perfil;

public record PerfilDTO(Integer codigo, String nome, String descricao) {

    public PerfilDTO(Perfil perfil) {
        this(perfil.getCodigo(), perfil.getNome(), perfil.getDescricao());
    }

    public Perfil toEntity() {
        Perfil perfil = new Perfil();
        perfil.setCodigo(codigo);
        perfil.setNome(nome);
        perfil.setDescricao(descricao);
        return perfil;
    }
}