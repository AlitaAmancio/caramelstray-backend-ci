package br.com.AllTallent.controller;

import br.com.AllTallent.dto.AreaDTO;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.repository.AreaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/area") 
public class AreaController {

    private final AreaRepository areaRepository;

    
    public AreaController(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    
    @PostMapping
    public ResponseEntity<AreaDTO> createArea(@RequestBody AreaDTO areaDTO) {
        Area novaArea = areaRepository.save(areaDTO.toEntity());
        return new ResponseEntity<>(new AreaDTO(novaArea), HttpStatus.CREATED);
    }

    
    @GetMapping
    public List<AreaDTO> getAllAreas() {
        return areaRepository.findAll().stream()
                .map(AreaDTO::new)
                .toList();
    }
}