package br.com.vestebemuai.notificacoes.email;

import br.com.vestebemuai.notificacoes.evento.PedidoCriadoEvento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Monta o corpo do e-mail a partir do arquivo templates/email-confirmacao.html (RF06).
 * Marcadores: {{cliente}}, {{pedido}}, {{itens}} e {{total}}.
 */
@Component
public class ModeloEmail {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final String modelo;

    public ModeloEmail(@Value("${vestebemuai.email.modelo}") Resource arquivo) {
        try {
            this.modelo = arquivo.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Modelo de e-mail nao encontrado: " + arquivo, e);
        }
    }

    public String montar(PedidoCriadoEvento evento) {
        String itens = evento.itens().stream().map(this::linha).collect(Collectors.joining());
        return modelo
                .replace("{{cliente}}", HtmlUtils.htmlEscape(evento.clienteNome()))
                .replace("{{pedido}}", evento.pedidoId().toString())
                .replace("{{itens}}", itens)
                .replace("{{total}}", reais(evento.total()));
    }

    private String linha(PedidoCriadoEvento.Item item) {
        return "<tr><td>%s</td><td>%s</td><td>%s</td><td>%d</td><td>%s</td></tr>".formatted(
                HtmlUtils.htmlEscape(item.produto()), HtmlUtils.htmlEscape(item.tamanho()),
                HtmlUtils.htmlEscape(item.cor()), item.quantidade(), reais(item.precoUnitario()));
    }

    private String reais(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }
}
