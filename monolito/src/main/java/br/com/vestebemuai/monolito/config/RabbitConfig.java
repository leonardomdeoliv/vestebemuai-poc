package br.com.vestebemuai.monolito.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lado do produtor: o monolito so conhece o exchange "pedidos".
 * Quem quiser receber os eventos (hoje, o servico de notificacoes) cria a
 * propria fila e faz o binding. Ver ADR-002.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_PEDIDOS = "pedidos";
    public static final String ROTA_PEDIDO_CRIADO = "pedido.criado";

    @Bean
    TopicExchange exchangePedidos() {
        return new TopicExchange(EXCHANGE_PEDIDOS, true, false);
    }

    @Bean
    MessageConverter mensagemJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
