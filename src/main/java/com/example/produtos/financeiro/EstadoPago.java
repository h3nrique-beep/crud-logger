package com.example.produtos.financeiro;

/** Já baixado: só pode ser estornado (volta a pendente). Cancelar um pago exige estornar antes. */
class EstadoPago implements EstadoLancamento {
    @Override
    public void estornar(LancamentoFinanceiro l) {
        l.limparPagamento();
        l.mudarPara(StatusLancamento.PENDENTE);
    }
}
