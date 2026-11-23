package com.example.produtos.financeiro;

import java.math.BigDecimal;

/** Indicadores: pendentes a receber e a pagar, o que já entrou e saiu, saldo realizado e quantos vencidos. */
public record ResumoFinanceiro(BigDecimal aReceber, BigDecimal aPagar, BigDecimal recebido, BigDecimal pago,
                               BigDecimal saldo, long vencidos) {}
