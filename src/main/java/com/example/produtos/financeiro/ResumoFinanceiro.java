package com.example.produtos.financeiro;

import java.math.BigDecimal;

/**
 * Resposta do GET /lancamentos/resumo: pendentes a receber e a pagar,
 * o que já entrou e saiu, o saldo e quantos estão vencidos.
 */
public record ResumoFinanceiro(BigDecimal aReceber, BigDecimal aPagar, BigDecimal recebido, BigDecimal pago,
                               BigDecimal saldo, long vencidos) {}
