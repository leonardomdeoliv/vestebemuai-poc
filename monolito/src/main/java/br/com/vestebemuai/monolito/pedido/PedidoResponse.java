package br.com.vestebemuai.monolito.pedido;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PedidoResponse(
        UUID id,
        String clienteNome,
        String clienteEmail,
        StatusPedido status,
        BigDecimal total,
        OffsetDateTime criadoEm,
        List<Item> itens) {

    public record Item(String produto, String tamanho, String cor, int quantidade, BigDecimal precoUnitario) {
    }

    public static PedidoResponse de(Pedido pedido) {
        List<Item> itens = pedido.getItens().stream()
                .map(i -> new Item(i.getProduto(), i.getTamanho(), i.getCor(), i.getQuantidade(), i.getPrecoUnitario()))
                .toList();
        return new PedidoResponse(pedido.getId(), pedido.getClienteNome(), pedido.getClienteEmail(),
                pedido.getStatus(), pedido.getTotal(), pedido.getCriadoEm(), itens);
    }
}
