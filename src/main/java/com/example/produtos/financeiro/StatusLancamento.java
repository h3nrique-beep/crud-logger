package com.example.produtos.financeiro;

/**
 * Liga o padrão State ao banco: o banco grava só o nome (PENDENTE, PAGO, CANCELADO)
 * e cada nome aponta para a classe de estado que tem o comportamento.
 */
public enum StatusLancamento {
    PENDENTE(new EstadoPendente()),
    PAGO(new EstadoPago()),
    CANCELADO(new EstadoCancelado());

    private final EstadoLancamento estado;

    StatusLancamento(EstadoLancamento estado) { this.estado = estado; }

    EstadoLancamento estado() { return estado; }
}
