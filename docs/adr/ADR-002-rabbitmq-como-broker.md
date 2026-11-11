# ADR-002: Usar o RabbitMQ como broker do evento PedidoCriado

- **Status:** Aceita
- **Data:** 09/11/2026
- **Decisores:** Leonardo (arquiteto e desenvolvedor do projeto)
- **Relacionada a:** [ADR-001](ADR-001-strangler-fig-notificacoes.md)

## Contexto

A ADR-001 decidiu ligar o monolítico ao novo serviço de notificações por um evento (`PedidoCriado`)
em uma fila. Falta escolher o broker. Restrições do projeto: custo zero no desenvolvimento, tudo em
Docker na máquina do autor (RNF04), no máximo US$ 5 na AWS (RNF06), uma pessoa no time e 3 sprints.
O volume é pequeno (cerca de 1.200 pedidos por dia) e o que importa é **não perder a notificação**
e conseguir **reenviar e separar as mensagens com falha** (H2).

## Opções consideradas

| Opção | Vantagens | Limitações para este projeto |
|---|---|---|
| **RabbitMQ** | Leve (um contêiner), painel web pronto, DLQ nativa por argumento da fila, integração direta com Spring AMQP (reenvio por configuração) | Ferramenta nova para o autor (premissa P5) |
| Apache Kafka | Retenção e reprocessamento de log, alto volume | Mais pesado (broker + controlador), sem DLQ nativa, volume da loja não justifica |
| Amazon SQS | Gerenciado, DLQ nativa | Prende o desenvolvimento local à AWS; fora do escopo (serviços gerenciados) |
| Tabela no PostgreSQL como fila | Nada novo para operar | Polling, sem roteamento nem painel, reinventa o broker |

## Decisão

Usar o **RabbitMQ 3.13** com o Spring AMQP, nesta topologia:

- O monolítico publica no exchange `pedidos` (tipo *topic*) com a rota `pedido.criado`. Ele só conhece o exchange.
- O serviço de notificações declara a própria fila `notificacoes.pedido-criado`, ligada ao exchange pela rota.
  Outro consumidor futuro (por exemplo, estoque) cria a sua fila sem mudar o monolítico.
- Mensagens em JSON, com o `eventoId` também no `message_id` da mensagem. Cada lado tem a própria classe
  do evento: o serviço ignora o cabeçalho de tipo Java do produtor.

## Consequências

- **Positivas:** desacoplamento de tempo (o checkout não espera o e-mail, H1); a DLQ e o reenvio saem por
  configuração (ADR-003); o painel em `localhost:15672` serve de evidência e de observabilidade (RNF03).
- **Negativas:** mais um contêiner para operar; o produtor publica depois do commit do pedido, então se o
  broker estiver fora do ar nesse instante o pedido fica gravado, mas o evento se perde (registrado no log).
  O padrão *Transactional Outbox* resolve isso e fica como melhoria futura.
- **Na AWS (Sprint 3):** o RabbitMQ roda como contêiner na mesma EC2 (ADR-004), sem Amazon MQ.
