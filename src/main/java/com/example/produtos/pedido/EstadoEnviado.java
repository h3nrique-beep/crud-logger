package com.example.produtos.pedido;

/** Pedido a caminho: só pode ser entregue; não aceita mais cancelamento. */
class EstadoEnviado implements EstadoPedido {
    @Override
    public void entregar(Pedido pedido) { pedido.mudarPara(StatusPedido.ENTREGUE); }
}
