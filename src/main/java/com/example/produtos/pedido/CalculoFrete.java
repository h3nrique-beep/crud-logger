package com.example.produtos.pedido;

import com.example.produtos.Endereco;

/**
 * Strategy: uma forma de calcular o frete. Cada implementação é um bean Spring;
 * o PedidoService recebe todas e escolhe pelo tipo, então um frete novo é só uma classe nova.
 */
public interface CalculoFrete {
    /** UF de onde a loja despacha. */
    String UF_LOJA = "PR";

    /** Identificador usado no pedido (ex.: "PADRAO"). */
    String tipo();

    double calcular(double subtotal, Endereco destino);
}
