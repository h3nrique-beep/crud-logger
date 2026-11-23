package com.example.produtos.financeiro;

import com.example.produtos.AppLogger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Regras do financeiro: valida o lançamento, escolhe a regra de pagamento (Strategy)
 * e aplica as transições de estado (State), registrando tudo no AppLogger.
 */
@Service
public class FinanceiroService {
    private final LancamentoRepository repo;
    private final Map<String, RegraPagamento> regras;
    private final AppLogger log = AppLogger.getInstance();

    public FinanceiroService(LancamentoRepository repo, List<RegraPagamento> regras) {
        this.repo = repo;
        this.regras = regras.stream().collect(Collectors.toMap(RegraPagamento::tipo, Function.identity()));
    }

    @Transactional
    public LancamentoFinanceiro criar(NovoLancamento req) {
        log.debug("criar lançamento tipo=" + req.tipo() + " valor=" + req.valor() + " vencimento=" + req.vencimento()
                + " regra=" + req.regraPagamento());
        if (req.tipo() == null) throw recusar(HttpStatus.BAD_REQUEST, "tipo obrigatório: use " + List.of(TipoLancamento.values()));
        if (req.descricao() == null || req.descricao().isBlank()) throw recusar(HttpStatus.BAD_REQUEST, "descrição obrigatória");
        if (req.valor() == null || req.valor().signum() <= 0) throw recusar(HttpStatus.BAD_REQUEST, "valor deve ser maior que zero");
        if (req.vencimento() == null) throw recusar(HttpStatus.BAD_REQUEST, "vencimento obrigatório");
        String regra = req.regraPagamento() == null ? "VALOR_FIXO" : req.regraPagamento();
        if (!regras.containsKey(regra))
            throw recusar(HttpStatus.BAD_REQUEST, "regraPagamento inválida: " + regra + "; use " + regras.keySet());

        String categoria = req.categoria() == null || req.categoria().isBlank() ? "Geral" : req.categoria().trim();
        LancamentoFinanceiro salvo = repo.save(new LancamentoFinanceiro(
                req.tipo(), req.descricao().trim(), categoria, req.valor(), req.vencimento(), regra));
        log.info("lançamento criado id=" + salvo.getId() + " " + salvo.getTipo() + " " + salvo.getValor()
                + " vence " + salvo.getVencimento() + " regra=" + regra);
        return salvo;
    }

    public List<LancamentoFinanceiro> listar(StatusLancamento status) {
        List<LancamentoFinanceiro> todos = status == null ? repo.findAll()
                : repo.findAll().stream().filter(l -> l.getStatus() == status).toList();
        log.info("listados " + todos.size() + " lançamentos" + (status == null ? "" : " " + status));
        return todos;
    }

    public LancamentoFinanceiro buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> recusar(HttpStatus.NOT_FOUND, "lançamento não encontrado id=" + id));
    }

    /** Baixa = pagar (despesa) ou receber (receita). A regra do lançamento calcula multa, juros ou desconto. */
    @Transactional
    public LancamentoFinanceiro baixar(Long id, LocalDate pagamentoEm) {
        LocalDate data = pagamentoEm == null ? LocalDate.now() : pagamentoEm;
        if (data.isAfter(LocalDate.now())) throw recusar(HttpStatus.BAD_REQUEST, "data de pagamento não pode ser futura");
        return transicionar(id, "baixar", l -> {
            RegraPagamento regra = regras.get(l.getRegraPagamento());
            BigDecimal ajuste = regra.ajuste(l.getValor(), l.getVencimento(), data);
            l.baixar(data, ajuste);
            log.info("baixa " + regra.tipo() + " em " + data + ": " + l.getValor() + " ajuste " + ajuste + " = " + l.getValorPago());
        });
    }

    @Transactional public LancamentoFinanceiro estornar(Long id) { return transicionar(id, "estornar", LancamentoFinanceiro::estornar); }
    @Transactional public LancamentoFinanceiro cancelar(Long id) { return transicionar(id, "cancelar", LancamentoFinanceiro::cancelar); }

    public ResumoFinanceiro resumo() {
        List<LancamentoFinanceiro> todos = repo.findAll();
        BigDecimal aReceber = somar(todos, TipoLancamento.RECEITA, StatusLancamento.PENDENTE, LancamentoFinanceiro::getValor);
        BigDecimal aPagar = somar(todos, TipoLancamento.DESPESA, StatusLancamento.PENDENTE, LancamentoFinanceiro::getValor);
        BigDecimal recebido = somar(todos, TipoLancamento.RECEITA, StatusLancamento.PAGO, LancamentoFinanceiro::getValorPago);
        BigDecimal pago = somar(todos, TipoLancamento.DESPESA, StatusLancamento.PAGO, LancamentoFinanceiro::getValorPago);
        long vencidos = todos.stream().filter(LancamentoFinanceiro::isVencido).count();
        ResumoFinanceiro r = new ResumoFinanceiro(aReceber, aPagar, recebido, pago, recebido.subtract(pago), vencidos);
        log.info("resumo: a receber " + aReceber + ", a pagar " + aPagar + ", saldo " + r.saldo() + ", vencidos " + vencidos);
        return r;
    }

    private BigDecimal somar(List<LancamentoFinanceiro> todos, TipoLancamento tipo, StatusLancamento status,
                             Function<LancamentoFinanceiro, BigDecimal> campo) {
        Predicate<LancamentoFinanceiro> filtro = l -> l.getTipo() == tipo && l.getStatus() == status;
        return todos.stream().filter(filtro).map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LancamentoFinanceiro transicionar(Long id, String operacao, Consumer<LancamentoFinanceiro> acao) {
        log.debug(operacao + " lançamento id=" + id);
        LancamentoFinanceiro l = buscar(id);
        StatusLancamento antes = l.getStatus();
        try {
            acao.accept(l);
        } catch (TransicaoInvalidaException e) {
            log.erro(e.getMessage());
            throw e;
        }
        log.info("lançamento " + id + ": " + antes + " -> " + l.getStatus());
        return repo.save(l);
    }

    private ResponseStatusException recusar(HttpStatus status, String motivo) {
        log.erro(motivo);
        return new ResponseStatusException(status, motivo);
    }
}
