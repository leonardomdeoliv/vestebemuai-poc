# Estudo dirigido de RabbitMQ (premissa P5)

Resumo do que é preciso saber de RabbitMQ para a Sprint 2, escrito para quem nunca usou.

## A ideia em uma frase

O RabbitMQ é um **correio entre programas**. Em vez de o monolítico chamar o servidor de e-mail e ficar
esperando, ele deixa uma carta (a **mensagem**) no correio e segue a vida. Outro programa (o serviço de
notificações) passa no correio, pega a carta e faz o envio quando puder.

## As peças

| Peça | Analogia | No nosso projeto |
|---|---|---|
| **Producer** (produtor) | Quem posta a carta | Monolítico, ao concluir o pedido |
| **Message** (mensagem) | A carta | Evento `PedidoCriado` em JSON |
| **Exchange** | A agência que separa as cartas | `pedidos.exchange` (tipo *topic*) |
| **Routing key** | O CEP escrito no envelope | `pedido.criado` |
| **Binding** | A regra "CEP tal vai para tal caixa" | `pedido.criado` → fila `notificacoes.pedido-criado` |
| **Queue** (fila) | A caixa postal | `notificacoes.pedido-criado` |
| **Consumer** (consumidor) | Quem abre a caixa e lê a carta | Serviço de notificações |
| **Ack** | O recibo de entrega | O consumidor confirma depois que o e-mail saiu |
| **DLQ** (*dead letter queue*) | A caixa de "cartas devolvidas" | `notificacoes.dlq`, depois de 3 tentativas |

```
Monolítico --(PedidoCriado, chave pedido.criado)--> [pedidos.exchange] --> [notificacoes.pedido-criado] --> Serviço de notificações --> Mailpit
                                                                                     |
                                                                         3 falhas    v
                                                                              [notificacoes.dlq]
```

## Por que isso resolve o problema da linha de base

- O checkout só precisa que o RabbitMQ aceite a mensagem (milissegundos). Se o servidor de e-mail cair,
  a mensagem **fica guardada na fila** em vez de derrubar o pedido (H1).
- A mensagem só sai da fila quando o consumidor manda o **ack**. Se o envio falhar, ela volta e é tentada
  de novo, com intervalo crescente (H2, RF04).
- Depois de 3 falhas, ela vai para a **DLQ**, de onde pode ser reprocessada sem se perder (RF05).
- Como uma mensagem pode ser entregue mais de uma vez, o consumidor guarda o id dos eventos já tratados
  para não mandar e-mail duplicado (**idempotência**, RNF02).

## Como o Spring faz isso (Spring AMQP)

- `spring-boot-starter-amqp` traz o `RabbitTemplate` (para publicar) e o `@RabbitListener` (para consumir).
- Exchange, fila e binding são declarados como `@Bean` e criados automaticamente no RabbitMQ ao subir a aplicação.
- O reenvio com intervalo crescente é configuração, sem código (premissa P4):

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2s
          multiplier: 2
        default-requeue-rejected: false   # depois das tentativas, vai para a DLQ
```

## Para ver na prática (já funciona na Sprint 1)

1. `docker compose up -d`
2. Abrir http://localhost:15672 (usuário e senha `vestebemuai`).
3. Na aba **Queues and Streams**, criar uma fila `teste`; em **Exchanges > amq.default**, publicar uma mensagem
   com routing key `teste`; voltar na fila e usar **Get messages** para lê-la. É exatamente o que o código fará.

## Fontes

- Tutoriais oficiais do RabbitMQ (Java): https://www.rabbitmq.com/tutorials
- Spring AMQP Reference: https://docs.spring.io/spring-amqp/reference/
- Guia "Messaging with RabbitMQ": https://spring.io/guides/gs/messaging-rabbitmq
