package br.com.vestebemuai.monolito.notificacao;

import br.com.vestebemuai.monolito.pedido.ItemPedido;
import br.com.vestebemuai.monolito.pedido.Pedido;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Modulo de notificacoes que hoje vive dentro do monolitico.
 * E este modulo que sera extraido para um microsservico na Sprint 2.
 */
@Service
public class EmailConfirmacaoService {

    private static final Logger log = LoggerFactory.getLogger(EmailConfirmacaoService.class);

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailConfirmacaoService(JavaMailSender mailSender,
                                   @Value("${vestebemuai.email.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    public void enviar(Pedido pedido) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(pedido.getClienteEmail());
            helper.setSubject("VesteBemUai - Pedido confirmado");
            helper.setText(corpo(pedido), true);
            mailSender.send(mensagem);
            log.info("E-mail de confirmacao do pedido {} enviado para {}", pedido.getId(), pedido.getClienteEmail());
        } catch (MessagingException e) {
            throw new MailSendException("Falha ao montar o e-mail do pedido " + pedido.getId(), e);
        }
    }

    private String corpo(Pedido pedido) {
        String itens = pedido.getItens().stream()
                .map(this::linha)
                .collect(Collectors.joining());
        return """
                <h2>Olá, %s!</h2>
                <p>Recebemos o seu pedido <b>%s</b>.</p>
                <table border="1" cellpadding="6" cellspacing="0">
                  <tr><th>Produto</th><th>Tamanho</th><th>Cor</th><th>Qtd.</th><th>Preço</th></tr>
                  %s
                </table>
                <p><b>Total: %s</b></p>
                <p>Obrigado por comprar na VesteBemUai.</p>
                """.formatted(pedido.getClienteNome(), pedido.getId(), itens, reais(pedido.getTotal()));
    }

    private String linha(ItemPedido item) {
        return "<tr><td>%s</td><td>%s</td><td>%s</td><td>%d</td><td>%s</td></tr>"
                .formatted(item.getProduto(), item.getTamanho(), item.getCor(), item.getQuantidade(), reais(item.getPrecoUnitario()));
    }

    private String reais(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }
}
