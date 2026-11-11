package br.com.vestebemuai.notificacoes.evento;

import br.com.vestebemuai.notificacoes.config.RabbitConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

/**
 * Consome o evento PedidoCriado da fila notificacoes.pedido-criado (RF03).
 *
 * Em caso de falha a excecao sobe, o Spring AMQP tenta de novo com intervalo
 * crescente (2 s, 4 s, 8 s; ver application.yml) e, esgotadas as tentativas,
 * a mensagem vai para a DLQ (RF04 e RF05).
 */
@Component
public class PedidoCriadoListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoCriadoListener.class);
    private static final int TOTAL_TENTATIVAS = 4;

    private final NotificacaoPedidoService notificacao;
    private final MeterRegistry metricas;

    public PedidoCriadoListener(NotificacaoPedidoService notificacao, MeterRegistry metricas) {
        this.notificacao = notificacao;
        this.metricas = metricas;
    }

    @RabbitListener(queues = RabbitConfig.FILA)
    public void aoReceber(PedidoCriadoEvento evento) {
        int tentativa = tentativaAtual();
        log.info("PedidoCriado recebido: evento {} pedido {} (tentativa {} de {})",
                evento.eventoId(), evento.pedidoId(), tentativa, TOTAL_TENTATIVAS);
        try {
            notificacao.notificar(evento);
        } catch (RuntimeException e) {
            String causa = NestedExceptionUtils.getMostSpecificCause(e).toString();
            metricas.counter("notificacoes.emails.falhas").increment();
            if (tentativa < TOTAL_TENTATIVAS) {
                log.warn("Tentativa {} de {} falhou para o pedido {}: {}. Nova tentativa em breve.",
                        tentativa, TOTAL_TENTATIVAS, evento.pedidoId(), causa);
            } else {
                log.error("Tentativa {} de {} falhou para o pedido {}: {}. Mensagem enviada para a DLQ.",
                        tentativa, TOTAL_TENTATIVAS, evento.pedidoId(), causa);
            }
            throw e;
        }
    }

    private int tentativaAtual() {
        RetryContext contexto = RetrySynchronizationManager.getContext();
        return contexto == null ? 1 : contexto.getRetryCount() + 1;
    }
}
