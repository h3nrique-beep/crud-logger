package com.example.produtos.financeiro;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EstadoLancamentoTest {
    private final LocalDate venc = LocalDate.of(2026, 11, 10);

    private LancamentoFinanceiro novo() {
        return new LancamentoFinanceiro(TipoLancamento.DESPESA, "Aluguel", "Fixas", new BigDecimal("1000"), venc, "MULTA_JUROS");
    }

    @Test
    void baixaRegistraPagamentoComAjuste() {
        LancamentoFinanceiro l = novo();
        l.baixar(venc.plusDays(10), new BigDecimal("23.30"));
        assertEquals(StatusLancamento.PAGO, l.getStatus());
        assertEquals(new BigDecimal("1023.30"), l.getValorPago());
        assertEquals(venc.plusDays(10), l.getPagamentoEm());
    }

    @Test
    void naoBaixaDuasVezes() {
        LancamentoFinanceiro l = novo();
        l.baixar(venc, BigDecimal.ZERO);
        assertThrows(TransicaoInvalidaException.class, () -> l.baixar(venc, BigDecimal.ZERO));
    }

    @Test
    void estornoVoltaParaPendenteELimpaPagamento() {
        LancamentoFinanceiro l = novo();
        l.baixar(venc, BigDecimal.ZERO);
        l.estornar();
        assertEquals(StatusLancamento.PENDENTE, l.getStatus());
        assertNull(l.getValorPago());
        assertNull(l.getPagamentoEm());
    }

    @Test
    void pendenteNaoPodeSerEstornadoEPagoNaoPodeSerCancelado() {
        LancamentoFinanceiro pendente = novo();
        assertThrows(TransicaoInvalidaException.class, pendente::estornar);

        LancamentoFinanceiro pago = novo();
        pago.baixar(venc, BigDecimal.ZERO);
        assertThrows(TransicaoInvalidaException.class, pago::cancelar);
    }

    @Test
    void canceladoNaoAceitaNada() {
        LancamentoFinanceiro l = novo();
        l.cancelar();
        assertEquals(StatusLancamento.CANCELADO, l.getStatus());
        assertThrows(TransicaoInvalidaException.class, () -> l.baixar(venc, BigDecimal.ZERO));
        assertThrows(TransicaoInvalidaException.class, l::estornar);
        assertThrows(TransicaoInvalidaException.class, l::cancelar);
    }
}
