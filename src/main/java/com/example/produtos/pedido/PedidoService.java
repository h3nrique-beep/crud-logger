package com.example.produtos.pedido;

import com.example.produtos.AppLogger;
import com.example.produtos.ConsultaCep;
import com.example.produtos.Endereco;
import com.example.produtos.Produto;
import com.example.produtos.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras do pedido: valida, consulta o CEP (pelo CepProxy, que usa o Adapter configurado),
 * calcula o frete pela Strategy escolhida e aplica as transições de estado.
 */
@Service
public class PedidoService {
    private final PedidoRepository repo;
    private final ProdutoRepository produtos;
    private final ConsultaCep consultaCep;
    private final Map<String, CalculoFrete> fretes;
    private final AppLogger log = AppLogger.getInstance();

    public PedidoService(PedidoRepository repo, ProdutoRepository produtos, ConsultaCep consultaCep,
                         List<CalculoFrete> fretes) {
        this.repo = repo;
        this.produtos = produtos;
        this.consultaCep = consultaCep;
        this.fretes = fretes.stream().collect(Collectors.toMap(CalculoFrete::tipo, Function.identity()));
    }

    @Transactional
    public Pedido criar(NovoPedido req) {
        log.debug("criar pedido produto=" + req.produtoId() + " qtd=" + req.quantidade()
                + " cep=" + req.cep() + " frete=" + req.tipoFrete());
        if (req.quantidade() <= 0) throw recusar(HttpStatus.BAD_REQUEST, "quantidade deve ser maior que zero");
        CalculoFrete estrategia = fretes.get(req.tipoFrete());
        if (estrategia == null)
            throw recusar(HttpStatus.BAD_REQUEST, "tipoFrete inválido: " + req.tipoFrete() + "; use " + fretes.keySet());
        Produto produto = (req.produtoId() == null ? null : produtos.findById(req.produtoId()).orElse(null));
        if (produto == null) throw recusar(HttpStatus.NOT_FOUND, "produto não encontrado id=" + req.produtoId());

        Endereco destino = consultaCep.buscar(req.cep());
        double subtotal = Math.round(produto.getPreco() * req.quantidade() * 100) / 100.0;
        double frete = estrategia.calcular(subtotal, destino);
        log.info("frete " + estrategia.tipo() + " para " + destino.uf() + " sobre " + subtotal + " = " + frete);

        Pedido salvo = repo.save(new Pedido(produto, req.quantidade(), destino, estrategia.tipo(), subtotal, frete));
        log.info("pedido criado id=" + salvo.getId() + " total=" + salvo.getTotal() + " status=" + salvo.getStatus());
        return salvo;
    }

    public List<Pedido> listar() {
        List<Pedido> todos = repo.findAll();
        log.info("listados " + todos.size() + " pedidos");
        return todos;
    }

    public Pedido buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> recusar(HttpStatus.NOT_FOUND, "pedido não encontrado id=" + id));
    }

    @Transactional public Pedido pagar(Long id) { return transicionar(id, "pagar", Pedido::pagar); }
    @Transactional public Pedido enviar(Long id) { return transicionar(id, "enviar", Pedido::enviar); }
    @Transactional public Pedido entregar(Long id) { return transicionar(id, "entregar", Pedido::entregar); }
    @Transactional public Pedido cancelar(Long id) { return transicionar(id, "cancelar", Pedido::cancelar); }

    private Pedido transicionar(Long id, String operacao, Consumer<Pedido> acao) {
        log.debug(operacao + " pedido id=" + id);
        Pedido pedido = buscar(id);
        StatusPedido antes = pedido.getStatus();
        try {
            acao.accept(pedido);
        } catch (TransicaoInvalidaException e) {
            log.erro(e.getMessage());
            throw e;
        }
        log.info("pedido " + id + ": " + antes + " -> " + pedido.getStatus());
        return repo.save(pedido);
    }

    private ResponseStatusException recusar(HttpStatus status, String motivo) {
        log.erro(motivo);
        return new ResponseStatusException(status, motivo);
    }
}
