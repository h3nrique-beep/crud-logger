package com.example.produtos.pedido;

import com.example.produtos.Endereco;
import org.springframework.stereotype.Component;

/** Entrega rápida: sem frete grátis; R$ 30 dentro do estado da loja, R$ 60 fora, mais 5% do subtotal (seguro). */
@Component
public class FreteExpresso implements CalculoFrete {
    @Override
    public String tipo() { return "EXPRESSO"; }

    @Override
    public double calcular(double subtotal, Endereco destino) {
        double base = UF_LOJA.equals(destino.uf()) ? 30 : 60;
        return Math.round((base + subtotal * 0.05) * 100) / 100.0;
    }
}
