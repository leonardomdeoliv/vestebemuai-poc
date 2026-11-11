package br.com.vestebemuai.monolito.pedido;

/** Como o checkout notifica o cliente (propriedade vestebemuai.notificacao.modo). */
public enum ModoNotificacao {
    /** Publica o evento PedidoCriado no RabbitMQ (Sprint 2). */
    FILA,
    /** Envia o e-mail dentro do checkout, como na Sprint 1. */
    SINCRONO
}
