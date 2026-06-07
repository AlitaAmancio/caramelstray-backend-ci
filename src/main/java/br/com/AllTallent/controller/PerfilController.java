package br.com.AllTallent.controller;

import br.com.AllTallent.dto.PerfilDTO;
import br.com.AllTallent.model.Perfil;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.repository.PerfilRepository;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilRepository perfilRepository;

    public PerfilController(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    
    @PostMapping
    public ResponseEntity<PerfilDTO> createPerfil(@RequestBody PerfilDTO perfilDTO) {
        Perfil novoPerfil = perfilRepository.save(perfilDTO.toEntity());
        return new ResponseEntity<>(new PerfilDTO(novoPerfil), HttpStatus.CREATED);
    }

    
    @GetMapping
    public List<PerfilDTO> getAllPerfis() {
        return perfilRepository.findAll().stream()
                .map(PerfilDTO::new)
                .toList();
    }
}