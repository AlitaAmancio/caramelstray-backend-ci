package br.com.AllTallent.dto;

import br.com.AllTallent.model.Avaliacao;
import br.com.AllTallent.model.AvaliacaoFuncionario;
import br.com.AllTallent.model.RespostaColaborador; 

import java.util.Collections;
import java.util.List;

public record AvaliacaoRevisaoDTO(
    Long avaliacaoFuncionarioCodigo,
    String nomeFuncionario,
    String tituloAvaliacao,
    String comentarioColaborador,
    String statusAtual,
    List<PerguntaComRespostaDTO> perguntasComRespostas 
) {
    public AvaliacaoRevisaoDTO(AvaliacaoFuncionario instancia, Avaliacao avaliacaoBase) {
        this(
            instancia.getCodigo(),
            (instancia.getFuncionario() != null) ? instancia.getFuncionario().getNomeCompleto() : null,
            avaliacaoBase.getTitulo(),
            instancia.getComentarioColaborador(),
            instancia.getResultadoStatus(),
            mapearPerguntas(instancia, avaliacaoBase)
        );
    }

    private static List<PerguntaComRespostaDTO> mapearPerguntas(AvaliacaoFuncionario instancia, Avaliacao avaliacaoBase) {
        if (avaliacaoBase.getPerguntas() == null) {
            return Collections.emptyList();
        }

        List<RespostaColaborador> respostas = (instancia.getRespostas() != null) 
                ? List.copyOf(instancia.getRespostas()) 
                : Collections.emptyList();

        return avaliacaoBase.getPerguntas().stream()
                .map(pergunta -> new PerguntaComRespostaDTO(pergunta, respostas))
                .toList();
    }
}