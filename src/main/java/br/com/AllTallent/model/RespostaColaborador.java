package br.com.AllTallent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_cad_resposta_colaborador")
public class RespostaColaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Seu banco usa sequence, mas identity deve funcionar se estiver configurado
    private Long codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_funcionario_avaliacao", nullable = false)
    private AvaliacaoFuncionario avaliacaoFuncionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_pergunta", nullable = false)
    private Pergunta pergunta;

    @Column(name = "resposta_texto", columnDefinition = "TEXT")
    private String respostaTexto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_pergunta_opcao_selecionada")
    private PerguntaOpcao perguntaOpcaoSelecionada; 

    // Métodos auxiliares para compatibilidade com código antigo (se precisar)
    public PerguntaOpcao getOpcaoSelecionada() {
        return this.perguntaOpcaoSelecionada;
    }

    public void setOpcaoSelecionada(PerguntaOpcao opcao) {
        this.perguntaOpcaoSelecionada = opcao;
    }
    
    // Método auxiliar para pegar o ID direto (útil para DTOs)
    public Long getCodigoPerguntaOpcaoSelecionada() {
        return (this.perguntaOpcaoSelecionada != null) ? this.perguntaOpcaoSelecionada.getCodigo() : null;
    }
}