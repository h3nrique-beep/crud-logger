package com.example.produtos.pedido;

import com.example.produtos.Endereco;
import org.springframework.stereotype.Component;

/** Cliente busca na loja: sem frete. */
@Component
public class RetiradaNaLoja implements CalculoFrete {
    @Override
    public String tipo() { return "RETIRADA"; }

    @Override
    public double calcular(double subtotal, Endereco destino) { return 0; }
}
