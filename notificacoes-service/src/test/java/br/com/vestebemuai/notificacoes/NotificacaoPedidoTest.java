package br.com.vestebemuai.notificacoes;

import br.com.vestebemuai.notificacoes.evento.EventoProcessadoRepository;
import br.com.vestebemuai.notificacoes.evento.NotificacaoPedidoService;
import br.com.vestebemuai.notificacoes.evento.PedidoCriadoEvento;
import br.com.vestebemuai.notificacoes.evento.PedidoCriadoListener;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras do servico de notificacoes sem broker real: o listener nao conecta no
 * RabbitMQ (auto-startup=false) e o SMTP e simulado.
 */
@SpringBootTest(properties = {"spring.rabbitmq.listener.simple.auto-startup=false", "management.health.mail.enabled=false"})
@ActiveProfiles("h2")
class NotificacaoPedidoTest {

    @Autowired
    PedidoCriadoListener listener;

    @Autowired
    NotificacaoPedidoService notificacao;

    @Autowired
    EventoProcessadoRepository processados;

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    RabbitTemplate rabbit;

    @BeforeEach
    void setUp() {
        processados.deleteAll();
        reset(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
    }

    private static PedidoCriadoEvento evento() {
        return new PedidoCriadoEvento(UUID.randomUUID(), UUID.randomUUID(), "Maria Teste", "maria@exemplo.com",
                List.of(new PedidoCriadoEvento.Item("Camiseta basica", "M", "Azul", 2, new BigDecimal("59.90")),
                        new PedidoCriadoEvento.Item("Bone aba curva", "U", "Preto", 1, new BigDecimal("39.90"))),
                new BigDecimal("159.70"), OffsetDateTime.now());
    }

    @Test
    @DisplayName("RF03: consome PedidoCriado e envia o e-mail com itens e total")
    void enviaEmailDeConfirmacao() throws Exception {
        PedidoCriadoEvento evento = evento();

        listener.aoReceber(evento);

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(enviado.capture());
        MimeMessage email = enviado.getValue();
        assertThat(email.getAllRecipients()[0].toString()).isEqualTo("maria@exemplo.com");
        assertThat(email.getSubject()).isEqualTo("VesteBemUai - Pedido confirmado");
        String corpo = (String) email.getContent();
        assertThat(corpo).contains("Maria Teste", evento.pedidoId().toString(), "Camiseta basica", "159,70");
        assertThat(processados.existsById(evento.eventoId())).isTrue();
    }

    @Test
    @DisplayName("RNF02: a mesma mensagem entregue duas vezes gera um unico e-mail")
    void mensagemDuplicadaGeraUmEmail() {
        PedidoCriadoEvento evento = evento();

        listener.aoReceber(evento);
        listener.aoReceber(evento);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertThat(processados.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF04: com o SMTP fora do ar a falha sobe para o reenvio e o evento nao fica marcado")
    void falhaNoSmtpPermiteNovaTentativa() {
        PedidoCriadoEvento evento = evento();
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> listener.aoReceber(evento)).isInstanceOf(MailSendException.class);
        assertThat(processados.existsById(evento.eventoId())).isFalse();

        // SMTP volta: a proxima tentativa envia o e-mail.
        doNothing().when(mailSender).send(any(MimeMessage.class));
        assertThat(notificacao.notificar(evento)).isTrue();
        assertThat(processados.existsById(evento.eventoId())).isTrue();
    }

    @Test
    @DisplayName("RNF02: eventos diferentes do mesmo cliente geram e-mails diferentes")
    void eventosDiferentesGeramEmails() {
        listener.aoReceber(evento());
        listener.aoReceber(evento());

        verify(mailSender, times(2)).send(any(MimeMessage.class));
        verify(mailSender, never()).send(any(MimeMessage[].class));
    }
}
