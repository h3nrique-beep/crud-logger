package com.example.produtos.financeiro;

/**
 * STATE: lançamento já pago ou recebido.
 * Pode: estornar (volta para PENDENTE).
 * Não pode: baixar de novo nem cancelar; para cancelar, estorne antes.
 */
class EstadoPago implements EstadoLancamento {
    @Override
    public void estornar(LancamentoFinanceiro l) {
        // Apaga os dados do pagamento e volta para pendente.
        l.limparPagamento();
        l.mudarPara(StatusLancamento.PENDENTE);
    }
}
