package br.com.vestebemuai.notificacoes;

import br.com.vestebemuai.notificacoes.evento.EventoProcessadoRepository;
import br.com.vestebemuai.notificacoes.evento.PedidoCriadoEvento;
import br.com.vestebemuai.notificacoes.evento.PedidoCriadoListener;
import br.com.vestebemuai.notificacoes.metricas.MetricasFilas;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/** S3.1 (RNF03): contadores e medidores publicados no Actuator. */
@SpringBootTest(properties = {"spring.rabbitmq.listener.simple.auto-startup=false", "management.health.mail.enabled=false"})
@ActiveProfiles("h2")
class MetricasTest {

    @Autowired
    PedidoCriadoListener listener;

    @Autowired
    EventoProcessadoRepository processados;

    @Autowired
    MeterRegistry registro;

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

    private double contador(String nome) {
        var c = registro.find(nome).counter();
        return c == null ? 0 : c.count();
    }

    private static PedidoCriadoEvento evento() {
        return new PedidoCriadoEvento(UUID.randomUUID(), UUID.randomUUID(), "Joana Teste", "joana@exemplo.com",
                List.of(new PedidoCriadoEvento.Item("Moletom canguru", "G", "Cinza", 1, new BigDecimal("149.90"))),
                new BigDecimal("149.90"), OffsetDateTime.now());
    }

    @Test
    @DisplayName("RNF03: envio e mensagem duplicada aparecem nos contadores")
    void contaEnviosEDuplicados() {
        double enviados = contador("notificacoes.emails.enviados");
        double duplicados = contador("notificacoes.eventos.duplicados");
        PedidoCriadoEvento evento = evento();

        listener.aoReceber(evento);
        listener.aoReceber(evento);

        assertThat(contador("notificacoes.emails.enviados")).isEqualTo(enviados + 1);
        assertThat(contador("notificacoes.eventos.duplicados")).isEqualTo(duplicados + 1);
    }

    @Test
    @DisplayName("RNF03: falha no SMTP aparece no contador de falhas")
    void contaFalhas() {
        double falhas = contador("notificacoes.emails.falhas");
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> listener.aoReceber(evento())).isInstanceOf(MailSendException.class);

        assertThat(contador("notificacoes.emails.falhas")).isEqualTo(falhas + 1);
    }

    @Test
    @DisplayName("RNF03: fila principal e DLQ tem medidor no Actuator")
    void medidoresDasFilas() {
        assertThat(registro.find(MetricasFilas.METRICA).tag("fila", "notificacoes.pedido-criado").gauge()).isNotNull();
        assertThat(registro.find(MetricasFilas.METRICA).tag("fila", "notificacoes.dlq").gauge()).isNotNull();
    }
}
