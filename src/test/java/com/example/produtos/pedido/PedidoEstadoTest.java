package com.example.produtos.pedido;

import com.example.produtos.Endereco;
import com.example.produtos.Produto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoEstadoTest {

    private Pedido novoPedido() {
        Produto p = new Produto();
        p.setNome("Caneta");
        p.setPreco(3.0);
        return new Pedido(p, 2, new Endereco("85810000", "", "", "Cascavel", "PR"), "PADRAO", 6.0, 15.0);
    }

    @Test
    void fluxoCompletoAteEntregue() {
        Pedido pedido = novoPedido();
        assertEquals(StatusPedido.CRIADO, pedido.getStatus());
        pedido.pagar();
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
        pedido.enviar();
        assertEquals(StatusPedido.ENVIADO, pedido.getStatus());
        pedido.entregar();
        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
        assertEquals(21.0, pedido.getTotal());
    }

    @Test
    void naoEnviaSemPagar() {
        Pedido pedido = novoPedido();
        assertThrows(TransicaoInvalidaException.class, pedido::enviar);
        assertEquals(StatusPedido.CRIADO, pedido.getStatus());
    }

    @Test
    void cancelaAntesDoEnvioMasNaoDepois() {
        Pedido criado = novoPedido();
        criado.cancelar();
        assertEquals(StatusPedido.CANCELADO, criado.getStatus());

        Pedido pago = novoPedido();
        pago.pagar();
        pago.cancelar();
        assertEquals(StatusPedido.CANCELADO, pago.getStatus());

        Pedido enviado = novoPedido();
        enviado.pagar();
        enviado.enviar();
        assertThrows(TransicaoInvalidaException.class, enviado::cancelar);
    }

    @Test
    void estadosFinaisNaoAceitamNada() {
        Pedido cancelado = novoPedido();
        cancelado.cancelar();
        assertThrows(TransicaoInvalidaException.class, cancelado::pagar);
        assertThrows(TransicaoInvalidaException.class, cancelado::cancelar);

        Pedido entregue = novoPedido();
        entregue.pagar();
        entregue.enviar();
        entregue.entregar();
        assertThrows(TransicaoInvalidaException.class, entregue::entregar);
        assertThrows(TransicaoInvalidaException.class, entregue::cancelar);
    }
}
