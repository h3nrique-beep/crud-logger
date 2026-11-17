package com.example.produtos.pedido;

/** Pedido recém-criado: aguarda pagamento; ainda pode ser cancelado. */
class EstadoCriado implements EstadoPedido {
    @Override
    public void pagar(Pedido pedido) { pedido.mudarPara(StatusPedido.PAGO); }

    @Override
    public void cancelar(Pedido pedido) { pedido.mudarPara(StatusPedido.CANCELADO); }
}
