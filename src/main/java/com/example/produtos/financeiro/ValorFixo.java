package com.example.produtos.financeiro;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** STRATEGY: paga sempre o valor original, em qualquer data. */
@Component
public class ValorFixo implements RegraPagamento {
    @Override
    public String tipo() { return "VALOR_FIXO"; }

    @Override
    public BigDecimal ajuste(BigDecimal valor, LocalDate vencimento, LocalDate pagamentoEm) { return BigDecimal.ZERO; }
}
