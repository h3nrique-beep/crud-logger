package com.example.produtos.financeiro;

/** O que é gravado no banco; cada status aponta para o objeto de estado que contém o comportamento. */
public enum StatusLancamento {
    PENDENTE(new EstadoPendente()),
    PAGO(new EstadoPago()),
    CANCELADO(new EstadoCancelado());

    private final EstadoLancamento estado;

    StatusLancamento(EstadoLancamento estado) { this.estado = estado; }

    EstadoLancamento estado() { return estado; }
}
