package com.example.produtos.financeiro;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Erro de operação fora de hora (ex.: baixar duas vezes).
 * O @ResponseStatus faz a API responder HTTP 409 (conflito) com o motivo.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(String mensagem) { super(mensagem); }
}
