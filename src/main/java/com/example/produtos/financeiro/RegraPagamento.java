package com.example.produtos.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Strategy: como o valor muda conforme a data do pagamento. Cada implementação é um
 * bean Spring; o FinanceiroService recebe todas e escolhe pelo tipo gravado no
 * lançamento, então uma regra nova é só uma classe nova.
 */
public interface RegraPagamento {
    /** Identificador gravado no lançamento (ex.: "MULTA_JUROS"). */
    String tipo();

    /** Valor a somar (positivo, encargos) ou subtrair (negativo, desconto) do valor original. */
    BigDecimal ajuste(BigDecimal valor, LocalDate vencimento, LocalDate pagamentoEm);
}
