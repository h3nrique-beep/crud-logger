package com.example.produtos.pedido;

/**
 * State: cada estado do pedido decide quais operações aceita.
 * Por padrão toda operação é negada; o estado concreto sobrescreve só as que permite.
 */
public interface EstadoPedido {

    default void pagar(Pedido pedido) { throw negar("pagar", pedido); }

    default void enviar(Pedido pedido) { throw negar("enviar", pedido); }

    default void entregar(Pedido pedido) { throw negar("entregar", pedido); }

    default void cancelar(Pedido pedido) { throw negar("cancelar", pedido); }

    private static TransicaoInvalidaException negar(String operacao, Pedido pedido) {
        return new TransicaoInvalidaException(
                "não é possível " + operacao + " o pedido " + pedido.getId() + " no estado " + pedido.getStatus());
    }
}
