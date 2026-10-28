package br.com.vestebemuai.monolito.pedido;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    private UUID id;

    private String clienteNome;

    private String clienteEmail;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    private BigDecimal total;

    private OffsetDateTime criadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(String clienteNome, String clienteEmail) {
        this.id = UUID.randomUUID();
        this.clienteNome = clienteNome;
        this.clienteEmail = clienteEmail;
        this.status = StatusPedido.CONFIRMADO;
        this.total = BigDecimal.ZERO;
        this.criadoEm = OffsetDateTime.now();
    }

    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        itens.add(item);
        total = total.add(item.subtotal());
    }

    public UUID getId() {
        return id;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }
}
