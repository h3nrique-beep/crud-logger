package com.example.produtos.financeiro;

import com.example.produtos.AppLogger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta de entrada da API: recebe o pedido HTTP e chama o service.
 * Não tem regra de negócio (Responsabilidade Única do SOLID).
 */
@RestController
@RequestMapping("/lancamentos")
public class FinanceiroController {
    private final FinanceiroService service;
    private final AppLogger log = AppLogger.getInstance();

    public FinanceiroController(FinanceiroService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoFinanceiro criar(@RequestBody NovoLancamento req) {
        log.debug("POST /lancamentos");
        return service.criar(req);
    }

    @GetMapping
    public List<LancamentoFinanceiro> listar(@RequestParam(required = false) StatusLancamento status) {
        log.debug("GET /lancamentos" + (status == null ? "" : "?status=" + status));
        return service.listar(status);
    }

    @GetMapping("/resumo")
    public ResumoFinanceiro resumo() {
        log.debug("GET /lancamentos/resumo");
        return service.resumo();
    }

    @GetMapping("/{id}")
    public LancamentoFinanceiro buscar(@PathVariable Long id) {
        log.debug("GET /lancamentos/" + id);
        return service.buscar(id);
    }

    @PostMapping("/{id}/baixar")
    public LancamentoFinanceiro baixar(@PathVariable Long id,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        log.debug("POST /lancamentos/" + id + "/baixar" + (data == null ? "" : "?data=" + data));
        return service.baixar(id, data);
    }

    @PostMapping("/{id}/estornar")
    public LancamentoFinanceiro estornar(@PathVariable Long id) {
        log.debug("POST /lancamentos/" + id + "/estornar");
        return service.estornar(id);
    }

    @PostMapping("/{id}/cancelar")
    public LancamentoFinanceiro cancelar(@PathVariable Long id) {
        log.debug("POST /lancamentos/" + id + "/cancelar");
        return service.cancelar(id);
    }
}
