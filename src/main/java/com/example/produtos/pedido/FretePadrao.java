package com.example.produtos.pedido;

import com.example.produtos.Endereco;
import org.springframework.stereotype.Component;

/** Entrega normal: grátis a partir de R$ 200; senão R$ 15 dentro do estado da loja e R$ 30 fora. */
@Component
public class FretePadrao implements CalculoFrete {
    static final double FRETE_GRATIS_A_PARTIR_DE = 200.0;

    @Override
    public String tipo() { return "PADRAO"; }

    @Override
    public double calcular(double subtotal, Endereco destino) {
        if (subtotal >= FRETE_GRATIS_A_PARTIR_DE) return 0;
        return UF_LOJA.equals(destino.uf()) ? 15 : 30;
    }
}
