package com.example.produtos.financeiro;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * STRATEGY: pagou com atraso, paga mais.
 * Conta: 2% de multa + 0,033% por dia de atraso. Em dia, não muda nada.
 * Exemplo: R$ 1.000 com 10 dias de atraso = 20,00 + 3,30 = R$ 1.023,30.
 */
@Component
public class MultaEJuros implements RegraPagamento {
    static final BigDecimal MULTA = new BigDecimal("0.02");
    static final BigDecimal JUROS_DIA = new BigDecimal("0.00033");

    @Override
    public String tipo() { return "MULTA_JUROS"; }

    @Override
    public BigDecimal ajuste(BigDecimal valor, LocalDate vencimento, LocalDate pagamentoEm) {
        long dias = ChronoUnit.DAYS.between(vencimento, pagamentoEm);
        if (dias <= 0) return BigDecimal.ZERO;
        BigDecimal juros = valor.multiply(JUROS_DIA).multiply(BigDecimal.valueOf(dias));
        return valor.multiply(MULTA).add(juros).setScale(2, RoundingMode.HALF_UP);
    }
}
