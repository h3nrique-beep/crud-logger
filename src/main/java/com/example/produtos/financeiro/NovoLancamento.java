package com.example.produtos.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados que chegam no POST /lancamentos para criar um lançamento. */
public record NovoLancamento(TipoLancamento tipo, String descricao, String categoria, BigDecimal valor,
                             LocalDate vencimento, String regraPagamento) {}
