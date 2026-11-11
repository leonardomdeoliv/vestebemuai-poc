package br.com.vestebemuai.notificacoes.dlq;

import br.com.vestebemuai.notificacoes.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Reprocessamento da DLQ (RF05): devolve as mensagens da notificacoes.dlq para a
 * fila principal, para serem tentadas de novo quando o SMTP voltar.
 *
 *   GET  /dlq              -> quantas mensagens estao na DLQ
 *   POST /dlq/reprocessar  -> move todas para a fila principal
 */
@RestController
@RequestMapping("/dlq")
public class DlqController {

    private static final Logger log = LoggerFactory.getLogger(DlqController.class);

    private final RabbitTemplate rabbit;
    private final AmqpAdmin admin;

    public DlqController(RabbitTemplate rabbit, AmqpAdmin admin) {
        this.rabbit = rabbit;
        this.admin = admin;
    }

    @GetMapping
    public Map<String, Object> situacao() {
        QueueInformation info = admin.getQueueInfo(RabbitConfig.DLQ);
        return Map.of("fila", RabbitConfig.DLQ, "mensagens", info == null ? 0 : info.getMessageCount());
    }

    @PostMapping("/reprocessar")
    public Map<String, Object> reprocessar() {
        int movidas = 0;
        Message mensagem;
        while ((mensagem = rabbit.receive(RabbitConfig.DLQ)) != null) {
            // Exchange padrao ("") com a rota = nome da fila: volta direto para a fila principal.
            rabbit.send("", RabbitConfig.FILA, mensagem);
            movidas++;
        }
        log.info("Reprocessamento da DLQ: {} mensagem(ns) devolvida(s) para {}", movidas, RabbitConfig.FILA);
        return Map.of("reprocessadas", movidas, "destino", RabbitConfig.FILA);
    }
}
