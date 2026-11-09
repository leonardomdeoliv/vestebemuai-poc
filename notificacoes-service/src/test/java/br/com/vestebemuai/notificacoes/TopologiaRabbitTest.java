package br.com.vestebemuai.notificacoes;

import br.com.vestebemuai.notificacoes.config.RabbitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** RF04 e RF05: configuracao de reenvio e DLQ. */
@SpringBootTest(properties = {"spring.rabbitmq.listener.simple.auto-startup=false", "management.health.mail.enabled=false"})
@ActiveProfiles("h2")
class TopologiaRabbitTest {

    @Autowired
    List<Queue> filas;

    @Autowired
    List<Binding> bindings;

    @Autowired
    RabbitProperties rabbit;

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("RF05: fila principal manda as mensagens rejeitadas para a DLQ")
    void filaPrincipalTemDeadLetter() {
        Queue principal = filas.stream().filter(f -> f.getName().equals(RabbitConfig.FILA)).findFirst().orElseThrow();
        assertThat(principal.isDurable()).isTrue();
        assertThat(principal.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitConfig.DLX)
                .containsEntry("x-dead-letter-routing-key", RabbitConfig.DLQ);
        assertThat(filas).extracting(Queue::getName).contains(RabbitConfig.DLQ);
        assertThat(bindings).anySatisfy(b -> {
            assertThat(b.getExchange()).isEqualTo("pedidos");
            assertThat(b.getRoutingKey()).isEqualTo("pedido.criado");
            assertThat(b.getDestination()).isEqualTo(RabbitConfig.FILA);
        });
    }

    @Test
    @DisplayName("RF04: 1 envio + 3 reenvios com intervalo crescente (2 s, 4 s, 8 s) e sem requeue")
    void reenvioComIntervaloCrescente() {
        RabbitProperties.ListenerRetry retry = rabbit.getListener().getSimple().getRetry();
        assertThat(retry.isEnabled()).isTrue();
        assertThat(retry.getMaxAttempts()).isEqualTo(4);
        assertThat(retry.getInitialInterval()).isEqualTo(Duration.ofSeconds(2));
        assertThat(retry.getMultiplier()).isEqualTo(2.0);
        assertThat(retry.getMaxInterval()).isEqualTo(Duration.ofSeconds(8));
        assertThat(rabbit.getListener().getSimple().getDefaultRequeueRejected()).isFalse();
    }
}
