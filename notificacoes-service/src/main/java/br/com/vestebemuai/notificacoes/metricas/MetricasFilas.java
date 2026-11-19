package br.com.vestebemuai.notificacoes.metricas;

import br.com.vestebemuai.notificacoes.config.RabbitConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.stereotype.Component;

/**
 * Observabilidade (S3.1, RNF03): publica no Actuator quantas mensagens estao
 * esperando na fila principal e na DLQ, lidas do RabbitMQ a cada consulta.
 *
 *   GET /actuator/metrics/notificacoes.fila.mensagens?tag=fila:notificacoes.pedido-criado
 *   GET /actuator/metrics/notificacoes.fila.mensagens?tag=fila:notificacoes.dlq
 *
 * Os contadores de envio ficam em NotificacaoPedidoService, PedidoCriadoListener e DlqController:
 *   notificacoes.emails.enviados, notificacoes.emails.falhas, notificacoes.mensagens.dlq,
 *   notificacoes.eventos.duplicados e notificacoes.dlq.reprocessadas.
 */
@Component
public class MetricasFilas {

    public static final String METRICA = "notificacoes.fila.mensagens";

    private final AmqpAdmin admin;

    /** Contadores registrados ja na subida, para aparecerem com 0 no Actuator antes do primeiro evento. */
    static final String[] CONTADORES = {"notificacoes.emails.enviados", "notificacoes.emails.falhas",
            "notificacoes.mensagens.dlq", "notificacoes.dlq.reprocessadas", "notificacoes.eventos.duplicados"};

    public MetricasFilas(AmqpAdmin admin, MeterRegistry registro) {
        this.admin = admin;
        for (String contador : CONTADORES) {
            registro.counter(contador);
        }
        for (String fila : new String[]{RabbitConfig.FILA, RabbitConfig.DLQ}) {
            Gauge.builder(METRICA, () -> mensagens(fila))
                    .tag("fila", fila)
                    .description("Mensagens esperando na fila")
                    .register(registro);
        }
    }

    double mensagens(String fila) {
        try {
            QueueInformation info = admin.getQueueInfo(fila);
            return info == null ? Double.NaN : info.getMessageCount();
        } catch (RuntimeException e) {
            return Double.NaN; // RabbitMQ fora do ar: a metrica fica sem valor, o servico segue
        }
    }
}
