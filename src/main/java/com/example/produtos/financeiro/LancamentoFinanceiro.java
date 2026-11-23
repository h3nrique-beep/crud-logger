package com.example.produtos.financeiro;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Uma conta a pagar ou a receber (tabela no banco).
 * Esta classe não decide o que é permitido: ela pergunta ao estado atual (padrão State).
 */
@Entity
public class LancamentoFinanceiro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private TipoLancamento tipo;
    private String descricao;
    private String categoria;
    @Column(precision = 14, scale = 2)
    private BigDecimal valor;
    private LocalDate vencimento;
    private String regraPagamento;
    @Enumerated(EnumType.STRING)
    private StatusLancamento status = StatusLancamento.PENDENTE;
    private LocalDate pagamentoEm;
    @Column(precision = 14, scale = 2)
    private BigDecimal ajuste;
    @Column(precision = 14, scale = 2)
    private BigDecimal valorPago;

    protected LancamentoFinanceiro() {}

    public LancamentoFinanceiro(TipoLancamento tipo, String descricao, String categoria, BigDecimal valor,
                                LocalDate vencimento, String regraPagamento) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
        this.vencimento = vencimento;
        this.regraPagamento = regraPagamento;
    }

    // STATE em ação: a decisão é do estado atual, sem nenhum if.
    public void baixar(LocalDate pagamentoEm, BigDecimal ajuste) { status.estado().baixar(this, pagamentoEm, ajuste); }
    public void estornar() { status.estado().estornar(this); }
    public void cancelar() { status.estado().cancelar(this); }

    /** Pendente com vencimento já passado. Calculado, não gravado: muda sozinho com a data. */
    public boolean isVencido() { return status == StatusLancamento.PENDENTE && vencimento.isBefore(LocalDate.now()); }

    // Só os estados (mesmo pacote) mexem nestes campos.
    void mudarPara(StatusLancamento novo) { this.status = novo; }

    void registrarPagamento(LocalDate em, BigDecimal ajuste) {
        this.pagamentoEm = em;
        this.ajuste = ajuste.setScale(2, RoundingMode.HALF_UP);
        this.valorPago = valor.add(this.ajuste);
    }

    void limparPagamento() {
        this.pagamentoEm = null;
        this.ajuste = null;
        this.valorPago = null;
    }

    // Só leitura: não há setters públicos, então ninguém muda o status por fora.
    public Long getId() { return id; }
    public TipoLancamento getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getVencimento() { return vencimento; }
    public String getRegraPagamento() { return regraPagamento; }
    public StatusLancamento getStatus() { return status; }
    public LocalDate getPagamentoEm() { return pagamentoEm; }
    public BigDecimal getAjuste() { return ajuste; }
    public BigDecimal getValorPago() { return valorPago; }
}
