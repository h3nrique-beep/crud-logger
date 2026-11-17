package com.example.produtos.pedido;

/** Corpo do POST /pedidos. */
public record NovoPedido(Long produtoId, int quantidade, String cep, String tipoFrete) {}
