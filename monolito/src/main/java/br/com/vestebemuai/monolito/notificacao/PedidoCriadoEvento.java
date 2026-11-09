package br.com.vestebemuai.monolito.notificacao;

import br.com.vestebemuai.monolito.pedido.Pedido;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Evento de integracao publicado no RabbitMQ quando um pedido e concluido (RF02).
 *
 * O eventoId identifica a mensagem de forma unica e e usado pelo servico de
 * notificacoes para nao enviar o mesmo e-mail duas vezes (RNF02).
 */
public record PedidoCriadoEvento(
        UUID eventoId,
        UUID pedidoId,
        String clienteNome,
        String clienteEmail,
        List<Item> itens,
        BigDecimal total,
        OffsetDateTime criadoEm) {

    public record Item(String produto, String tamanho, String cor, int quantidade, BigDecimal precoUnitario) {
    }

    public static PedidoCriadoEvento de(Pedido pedido) {
        List<Item> itens = pedido.getItens().stream()
                .map(i -> new Item(i.getProduto(), i.getTamanho(), i.getCor(), i.getQuantidade(), i.getPrecoUnitario()))
                .toList();
        return new PedidoCriadoEvento(UUID.randomUUID(), pedido.getId(), pedido.getClienteNome(),
                pedido.getClienteEmail(), itens, pedido.getTotal(), pedido.getCriadoEm());
    }
}
