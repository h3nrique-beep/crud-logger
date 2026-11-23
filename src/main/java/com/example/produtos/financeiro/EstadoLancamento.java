package com.example.produtos.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PADRÃO STATE (interface).
 * Problema: um lançamento só pode ser baixado, estornado ou cancelado em certos momentos.
 * Solução: cada estado é uma classe que diz o que aceita, em vez de vários "if (status == ...)".
 * Aqui tudo é proibido por padrão; cada estado libera só o que pode.
 */
public interface EstadoLancamento {

    default void baixar(LancamentoFinanceiro l, LocalDate pagamentoEm, BigDecimal ajuste) { throw negar("baixar", l); }

    default void estornar(LancamentoFinanceiro l) { throw negar("estornar", l); }

    default void cancelar(LancamentoFinanceiro l) { throw negar("cancelar", l); }

    // Monta a mensagem de erro que o usuário vê.
    private static TransicaoInvalidaException negar(String operacao, LancamentoFinanceiro l) {
        return new TransicaoInvalidaException(
                "não é possível " + operacao + " o lançamento " + l.getId() + " no estado " + l.getStatus());
    }
}
