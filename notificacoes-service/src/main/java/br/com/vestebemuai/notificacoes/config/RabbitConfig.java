package br.com.vestebemuai.notificacoes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do lado do consumidor (ADR-002 e ADR-003):
 *
 *   exchange "pedidos" (topic) --pedido.criado--> fila "notificacoes.pedido-criado"
 *   fila principal --(rejeitada apos 3 reenvios)--> exchange "notificacoes.dlx" --> fila "notificacoes.dlq"
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_PEDIDOS = "pedidos";
    public static final String ROTA_PEDIDO_CRIADO = "pedido.criado";
    public static final String FILA = "notificacoes.pedido-criado";
    public static final String DLX = "notificacoes.dlx";
    public static final String DLQ = "notificacoes.dlq";

    @Bean
    TopicExchange exchangePedidos() {
        return new TopicExchange(EXCHANGE_PEDIDOS, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue filaPedidoCriado() {
        return QueueBuilder.durable(FILA)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue dlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding bindingPedidoCriado(Queue filaPedidoCriado, TopicExchange exchangePedidos) {
        return BindingBuilder.bind(filaPedidoCriado).to(exchangePedidos).with(ROTA_PEDIDO_CRIADO);
    }

    @Bean
    Binding bindingDlq(Queue dlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(dlq).to(deadLetterExchange).with(DLQ);
    }

    /**
     * O monolito manda o nome da classe dele no cabecalho __TypeId__. O servico
     * ignora esse cabecalho e converte o JSON para o tipo do parametro do listener,
     * para que os dois lados nao dependam das classes um do outro.
     */
    @Bean
    MessageConverter mensagemJson(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter conversor = new Jackson2JsonMessageConverter(objectMapper);
        conversor.setAlwaysConvertToInferredType(true);
        return conversor;
    }
}
