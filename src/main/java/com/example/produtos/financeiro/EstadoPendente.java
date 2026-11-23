package com.example.produtos.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * STATE: lançamento em aberto.
 * Pode: baixar (vira PAGO) e cancelar (vira CANCELADO).
 * Não pode: estornar, porque ainda não foi pago.
 */
class EstadoPendente implements EstadoLancamento {
    @Override
    public void baixar(LancamentoFinanceiro l, LocalDate pagamentoEm, BigDecimal ajuste) {
        // Guarda a data e o valor pago, depois troca o estado.
        l.registrarPagamento(pagamentoEm, ajuste);
        l.mudarPara(StatusLancamento.PAGO);
    }

    @Override
    public void cancelar(LancamentoFinanceiro l) { l.mudarPara(StatusLancamento.CANCELADO); }
}
