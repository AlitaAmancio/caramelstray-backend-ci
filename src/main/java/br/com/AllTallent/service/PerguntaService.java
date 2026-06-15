package br.com.AllTallent.service; 

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import br.com.AllTallent.dto.PerguntaRequestDTO;
import br.com.AllTallent.dto.PerguntaResponseDTO;
import br.com.AllTallent.dto.OpcaoRequest;
import br.com.AllTallent.model.Competencia;
import br.com.AllTallent.model.Pergunta; 
import br.com.AllTallent.model.PerguntaOpcao;
import br.com.AllTallent.repository.CompetenciaRepository;
import br.com.AllTallent.repository.PerguntaRepository;
import jakarta.persistence.EntityNotFoundException;

@Slf4j
@Service
public class PerguntaService {

    private final PerguntaRepository perguntaRepository;
    private final CompetenciaRepository competenciaRepository;

    public PerguntaService(PerguntaRepository perguntaRepository, CompetenciaRepository competenciaRepository) {
        this.perguntaRepository = perguntaRepository;
        this.competenciaRepository = competenciaRepository;
    }

    @Transactional 
    public PerguntaResponseDTO criarPergunta(PerguntaRequestDTO dto) {
        Competencia competencia = competenciaRepository.findById(dto.competenciaCodigo())
                .orElseThrow(() -> new EntityNotFoundException("Competência não encontrada: " + dto.competenciaCodigo()));

        Pergunta novaPergunta = new Pergunta();
        novaPergunta.setDescricao(dto.pergunta());
        novaPergunta.setCompetencia(competencia);
        novaPergunta.setTipoPergunta(dto.tipoPergunta()); 

        // CORREÇÃO DO BUG: Verificação mais robusta do tipo (com ou sem acento)
        String tipo = dto.tipoPergunta() != null ? dto.tipoPergunta().toLowerCase() : "";
        boolean isMultipla = tipo.contains("múltipla") || tipo.contains("multipla");

        if (isMultipla && dto.opcoes() != null && !dto.opcoes().isEmpty()) {
            log.info(">>> Processing {} options received.", dto.opcoes().size());
            
            Set<PerguntaOpcao> opcoesSet = new HashSet<>();
            
            for (OpcaoRequest opRequest : dto.opcoes()) { 
                if (opRequest.descricao() != null && !opRequest.descricao().trim().isEmpty()) {
                    PerguntaOpcao opcao = new PerguntaOpcao();
                    
                    opcao.setDescricaoOpcao(opRequest.descricao().trim());
                    opcao.setIsCorreta(opRequest.isCorreta());
                    
                    // VINCULO IMPORTANTE: JPA precisa saber quem é o pai
                    opcao.setPergunta(novaPergunta); 
                    
                    opcoesSet.add(opcao);
                }
            }
            novaPergunta.setOpcoes(opcoesSet);
        } else {
             log.info(">>> Não é múltipla escolha ou não há opções válidas. Tipo recebido: " + tipo);
        }

        log.info(">>> Salvando Pergunta no repositório..."); 
        Pergunta perguntaSalva = perguntaRepository.save(novaPergunta);
        log.info(">>> Pergunta salva com código: " + perguntaSalva.getCodigo()); 

        return new PerguntaResponseDTO(perguntaSalva);
    }

    @Transactional(readOnly = true) 
    public List<PerguntaResponseDTO> listarTodas() {
        return perguntaRepository.findAll().stream()
                .map(PerguntaResponseDTO::new)
                .toList();
    }

     
    @Transactional(readOnly = true)
    public PerguntaResponseDTO buscarPorId(Long id) {
        return perguntaRepository.findById(id)
                .map(PerguntaResponseDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Pergunta não encontrada: " + id));
    }

    @Transactional
    public void deletarPergunta(Long id) {
        if (!perguntaRepository.existsById(id)) {
            throw new EntityNotFoundException("Pergunta não encontrada: " + id);
        }
        perguntaRepository.deleteById(id);
    }
}