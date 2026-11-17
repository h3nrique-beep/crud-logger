package com.example.produtos.pedido;

/** O que é gravado no banco; cada status aponta para o objeto de estado que contém o comportamento. */
public enum StatusPedido {
    CRIADO(new EstadoCriado()),
    PAGO(new EstadoPago()),
    ENVIADO(new EstadoEnviado()),
    ENTREGUE(new EstadoFinal()),
    CANCELADO(new EstadoFinal());

    private final EstadoPedido estado;

    StatusPedido(EstadoPedido estado) { this.estado = estado; }

    EstadoPedido estado() { return estado; }
}
