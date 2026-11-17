package com.example.produtos.pedido;

import com.example.produtos.AppLogger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Só traduz HTTP para chamadas do PedidoService; nenhuma regra aqui. */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService service;
    private final AppLogger log = AppLogger.getInstance();

    public PedidoController(PedidoService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido criar(@RequestBody NovoPedido req) {
        log.debug("POST /pedidos");
        return service.criar(req);
    }

    @GetMapping
    public List<Pedido> listar() {
        log.debug("GET /pedidos");
        return service.listar();
    }

    @GetMapping("/{id}")
    public Pedido buscar(@PathVariable Long id) {
        log.debug("GET /pedidos/" + id);
        return service.buscar(id);
    }

    @PostMapping("/{id}/pagar")
    public Pedido pagar(@PathVariable Long id) {
        log.debug("POST /pedidos/" + id + "/pagar");
        return service.pagar(id);
    }

    @PostMapping("/{id}/enviar")
    public Pedido enviar(@PathVariable Long id) {
        log.debug("POST /pedidos/" + id + "/enviar");
        return service.enviar(id);
    }

    @PostMapping("/{id}/entregar")
    public Pedido entregar(@PathVariable Long id) {
        log.debug("POST /pedidos/" + id + "/entregar");
        return service.entregar(id);
    }

    @PostMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable Long id) {
        log.debug("POST /pedidos/" + id + "/cancelar");
        return service.cancelar(id);
    }
}
