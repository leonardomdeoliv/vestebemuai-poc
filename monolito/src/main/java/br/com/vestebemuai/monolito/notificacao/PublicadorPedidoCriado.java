package br.com.vestebemuai.monolito.notificacao;

import br.com.vestebemuai.monolito.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica o evento PedidoCriado no exchange "pedidos" (RF02).
 *
 * A publicacao acontece so DEPOIS do commit da transacao do pedido: assim nunca
 * sai um evento de um pedido que sofreu rollback, e o checkout nao espera o envio
 * do e-mail. Se o RabbitMQ estiver fora do ar, o pedido continua gravado e a falha
 * fica no log (o padrao Outbox fica registrado como melhoria futura no ADR-003).
 */
@Component
public class PublicadorPedidoCriado {

    private static final Logger log = LoggerFactory.getLogger(PublicadorPedidoCriado.class);

    private final RabbitTemplate rabbit;

    public PublicadorPedidoCriado(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(PedidoCriadoEvento evento) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE_PEDIDOS, RabbitConfig.ROTA_PEDIDO_CRIADO, evento, m -> {
                m.getMessageProperties().setMessageId(evento.eventoId().toString());
                m.getMessageProperties().setType("PedidoCriado");
                return m;
            });
            log.info("Evento PedidoCriado {} publicado para o pedido {}", evento.eventoId(), evento.pedidoId());
        } catch (AmqpException e) {
            log.error("Pedido {} gravado, mas o evento PedidoCriado nao foi publicado: {}", evento.pedidoId(), e.getMessage());
        }
    }
}
