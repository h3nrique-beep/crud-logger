package com.example.produtos.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Em aberto: pode ser baixado (pago/recebido) ou cancelado. */
class EstadoPendente implements EstadoLancamento {
    @Override
    public void baixar(LancamentoFinanceiro l, LocalDate pagamentoEm, BigDecimal ajuste) {
        l.registrarPagamento(pagamentoEm, ajuste);
        l.mudarPara(StatusLancamento.PAGO);
    }

    @Override
    public void cancelar(LancamentoFinanceiro l) { l.mudarPara(StatusLancamento.CANCELADO); }
}
