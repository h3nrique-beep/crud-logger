package com.example.produtos.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PADRÃO STRATEGY (interface).
 * Problema: na baixa, o valor pode ter multa, desconto ou não mudar.
 * Solução: cada forma de calcular é uma classe. Para criar uma regra nova, basta criar
 * uma classe nova; o FinanceiroService não muda (Aberto/Fechado do SOLID).
 */
public interface RegraPagamento {
    /** Identificador gravado no lançamento (ex.: "MULTA_JUROS"). */
    String tipo();

    /** Valor a somar (positivo, encargos) ou subtrair (negativo, desconto) do valor original. */
    BigDecimal ajuste(BigDecimal valor, LocalDate vencimento, LocalDate pagamentoEm);
}
