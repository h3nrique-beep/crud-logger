package com.example.produtos.pedido;

import com.example.produtos.Endereco;
import com.example.produtos.Produto;
import jakarta.persistence.*;

/**
 * Pedido de um produto com entrega. Guarda uma cópia do nome e do preço do produto
 * (o preço do momento da compra vale, e o produto pode ser removido no CRUD sem quebrar o pedido).
 * As transições de status são delegadas ao objeto de estado (padrão State).
 */
@Entity
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long produtoId;
    private String produtoNome;
    private double precoUnitario;
    private int quantidade;
    private String cep;
    private String cidade;
    private String uf;
    private String tipoFrete;
    private double subtotal;
    private double frete;
    private double total;
    @Enumerated(EnumType.STRING)
    private StatusPedido status = StatusPedido.CRIADO;

    protected Pedido() {}

    public Pedido(Produto produto, int quantidade, Endereco destino, String tipoFrete, double subtotal, double frete) {
        this.produtoId = produto.getId();
        this.produtoNome = produto.getNome();
        this.precoUnitario = produto.getPreco();
        this.quantidade = quantidade;
        this.cep = destino.cep();
        this.cidade = destino.cidade();
        this.uf = destino.uf();
        this.tipoFrete = tipoFrete;
        this.subtotal = subtotal;
        this.frete = frete;
        this.total = Math.round((subtotal + frete) * 100) / 100.0;
    }

    public void pagar() { status.estado().pagar(this); }
    public void enviar() { status.estado().enviar(this); }
    public void entregar() { status.estado().entregar(this); }
    public void cancelar() { status.estado().cancelar(this); }

    /** Só os estados (mesmo pacote) mudam o status. */
    void mudarPara(StatusPedido novo) { this.status = novo; }

    public Long getId() { return id; }
    public Long getProdutoId() { return produtoId; }
    public String getProdutoNome() { return produtoNome; }
    public double getPrecoUnitario() { return precoUnitario; }
    public int getQuantidade() { return quantidade; }
    public String getCep() { return cep; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
    public String getTipoFrete() { return tipoFrete; }
    public double getSubtotal() { return subtotal; }
    public double getFrete() { return frete; }
    public double getTotal() { return total; }
    public StatusPedido getStatus() { return status; }
}
