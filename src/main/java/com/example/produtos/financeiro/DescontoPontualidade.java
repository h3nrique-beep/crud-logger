package com.example.produtos.financeiro;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * STRATEGY: pagou até o vencimento, ganha 5% de desconto.
 * Exemplo: R$ 500 pagos em dia = R$ 475. Depois do vencimento, valor cheio.
 */
@Component
public class DescontoPontualidade implements RegraPagamento {
    static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String tipo() { return "DESCONTO_PONTUALIDADE"; }

    @Override
    public BigDecimal ajuste(BigDecimal valor, LocalDate vencimento, LocalDate pagamentoEm) {
        if (pagamentoEm.isAfter(vencimento)) return BigDecimal.ZERO;
        return valor.multiply(DESCONTO).negate().setScale(2, RoundingMode.HALF_UP);
    }
}
