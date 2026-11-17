package com.example.produtos.pedido;

/** Pedido pago: pode ser enviado; ainda pode ser cancelado (estorno) antes do envio. */
class EstadoPago implements EstadoPedido {
    @Override
    public void enviar(Pedido pedido) { pedido.mudarPara(StatusPedido.ENVIADO); }

    @Override
    public void cancelar(Pedido pedido) { pedido.mudarPara(StatusPedido.CANCELADO); }
}
