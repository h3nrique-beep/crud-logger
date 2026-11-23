package com.example.produtos.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Corpo do POST /lancamentos. */
public record NovoLancamento(TipoLancamento tipo, String descricao, String categoria, BigDecimal valor,
                             LocalDate vencimento, String regraPagamento) {}
