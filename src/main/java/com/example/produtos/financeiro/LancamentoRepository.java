package com.example.produtos.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco. O Spring Data cria salvar, buscar e listar sozinho. */
public interface LancamentoRepository extends JpaRepository<LancamentoFinanceiro, Long> {}
