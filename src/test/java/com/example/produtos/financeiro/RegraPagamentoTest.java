package com.example.produtos.financeiro;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegraPagamentoTest {
    private final BigDecimal mil = new BigDecimal("1000.00");
    private final LocalDate venc = LocalDate.of(2026, 11, 10);

    @Test
    void multaEJurosSoComAtraso() {
        RegraPagamento r = new MultaEJuros();
        assertEquals(0, r.ajuste(mil, venc, venc).signum());
        assertEquals(0, r.ajuste(mil, venc, venc.minusDays(3)).signum());
        // 2% de multa (20,00) + 0,033% x 10 dias (3,30)
        assertEquals(new BigDecimal("23.30"), r.ajuste(mil, venc, venc.plusDays(10)));
    }

    @Test
    void descontoSoAteOVencimento() {
        RegraPagamento r = new DescontoPontualidade();
        assertEquals(new BigDecimal("-50.00"), r.ajuste(mil, venc, venc));
        assertEquals(new BigDecimal("-50.00"), r.ajuste(mil, venc, venc.minusDays(5)));
        assertEquals(0, r.ajuste(mil, venc, venc.plusDays(1)).signum());
    }

    @Test
    void valorFixoNuncaMuda() {
        assertEquals(0, new ValorFixo().ajuste(mil, venc, venc.plusDays(30)).signum());
    }
}
