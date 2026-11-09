package br.com.vestebemuai.notificacoes.evento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Contrato do evento PedidoCriado, do ponto de vista do servico de notificacoes. */
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
}
