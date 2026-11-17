package com.example.produtos.pedido;

import com.example.produtos.Endereco;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculoFreteTest {
    private final Endereco cascavelPr = new Endereco("85810000", "", "", "Cascavel", "PR");
    private final Endereco saoPauloSp = new Endereco("01001000", "", "", "São Paulo", "SP");

    @Test
    void padraoPorRegiaoEGratisAcimaDe200() {
        CalculoFrete f = new FretePadrao();
        assertEquals(15, f.calcular(100, cascavelPr));
        assertEquals(30, f.calcular(100, saoPauloSp));
        assertEquals(0, f.calcular(200, saoPauloSp));
    }

    @Test
    void expressoSemGratisComSeguro() {
        CalculoFrete f = new FreteExpresso();
        assertEquals(35, f.calcular(100, cascavelPr));
        assertEquals(75, f.calcular(300, saoPauloSp));
    }

    @Test
    void retiradaNaLojaNaoCobra() {
        assertEquals(0, new RetiradaNaLoja().calcular(100, saoPauloSp));
    }
}
