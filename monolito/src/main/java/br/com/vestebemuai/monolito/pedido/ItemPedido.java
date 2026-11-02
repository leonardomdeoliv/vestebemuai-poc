package br.com.vestebemuai.monolito.pedido;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    private String produto;

    private String tamanho;

    private String cor;

    private int quantidade;

    private BigDecimal precoUnitario;

    protected ItemPedido() {
    }

    public ItemPedido(String produto, String tamanho, String cor, int quantidade, BigDecimal precoUnitario) {
        this.produto = produto;
        this.tamanho = tamanho;
        this.cor = cor;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public String getProduto() {
        return produto;
    }

    public String getTamanho() {
        return tamanho;
    }

    public String getCor() {
        return cor;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
