package com.example.produtos.pedido;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Operação incompatível com o estado atual do pedido (ex.: enviar antes de pagar). Vira HTTP 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(String mensagem) { super(mensagem); }
}
