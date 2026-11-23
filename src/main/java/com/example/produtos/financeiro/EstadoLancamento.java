package com.example.produtos.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * State: cada estado do lançamento decide quais operações aceita.
 * Por padrão toda operação é negada; o estado concreto sobrescreve só as que permite.
 */
public interface EstadoLancamento {

    default void baixar(LancamentoFinanceiro l, LocalDate pagamentoEm, BigDecimal ajuste) { throw negar("baixar", l); }

    default void estornar(LancamentoFinanceiro l) { throw negar("estornar", l); }

    default void cancelar(LancamentoFinanceiro l) { throw negar("cancelar", l); }

    private static TransicaoInvalidaException negar(String operacao, LancamentoFinanceiro l) {
        return new TransicaoInvalidaException(
                "não é possível " + operacao + " o lançamento " + l.getId() + " no estado " + l.getStatus());
    }
}
