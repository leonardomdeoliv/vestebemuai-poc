# ADR-003: Reenvio com intervalo crescente, DLQ e idempotência no serviço de notificações

- **Status:** Aceita
- **Data:** 09/11/2026
- **Decisores:** Leonardo (arquiteto e desenvolvedor do projeto)
- **Relacionada a:** [ADR-002](ADR-002-rabbitmq-como-broker.md); requisitos RF04, RF05 e RNF02; hipótese H2

## Contexto

Com o e-mail fora do checkout (ADR-001), uma falha no SMTP não derruba mais o pedido, mas a mensagem
ainda precisa chegar ao cliente. O servidor de e-mail pode ficar fora do ar por segundos (reinício) ou
por horas. Precisamos de:

1. novas tentativas automáticas para falhas curtas (RF04);
2. um lugar seguro para as mensagens que continuam falhando, sem travar a fila (RF05);
3. garantia de que reentregas não mandem o mesmo e-mail duas vezes (RNF02).

## Opções consideradas

| Tema | Opção | Avaliação |
|---|---|---|
| Reenvio | Devolver a mensagem para a fila (*requeue*) a cada falha | Loop infinito e fila travada se o SMTP ficar fora do ar |
| Reenvio | **Retry do Spring AMQP com intervalo crescente, em memória** | Por configuração (P4); simples; segura a thread durante a espera, aceitável no volume da loja |
| Reenvio | Filas de espera com TTL (uma por intervalo) | Não segura a thread, mas exige mais filas e mais configuração |
| Falha final | Descartar e só logar | Perde a notificação |
| Falha final | **DLQ com reprocessamento manual** | Nada se perde; a equipe decide quando reprocessar |
| Duplicidade | Confiar em "exatamente uma vez" do broker | O RabbitMQ entrega "pelo menos uma vez" |
| Duplicidade | **Tabela de eventos processados pelo `eventoId`** | Simples e verificável no banco |

## Decisão

- **Reenvio:** 1 envio + 3 reenvios, esperando **2 s, 4 s e 8 s** (`max-attempts: 4`, `multiplier: 2`).
  Cada tentativa é registrada no log (`Tentativa N de 4 falhou...`).
- **DLQ:** a fila `notificacoes.pedido-criado` tem `x-dead-letter-exchange = notificacoes.dlx`. Esgotadas as
  tentativas, a mensagem é rejeitada sem voltar para a fila (`default-requeue-rejected: false`) e o RabbitMQ
  a move para `notificacoes.dlq`. `GET /dlq` mostra quantas há e `POST /dlq/reprocessar` devolve todas para a
  fila principal.
- **Idempotência:** antes de enviar, o serviço grava o `eventoId` na tabela `notificacoes.evento_processado`,
  na mesma transação do envio. Se o SMTP falha, a transação volta e a próxima tentativa envia; se a mesma
  mensagem chega de novo depois do envio, ela é ignorada e registrada no log.

## Consequências

- **Positivas:** nenhuma notificação se perde com o SMTP fora do ar; a DLQ mostra no painel o que falhou (RNF03);
  mensagens duplicadas não geram e-mail repetido.
- **Negativas:** durante as esperas (até 14 s) a thread do consumidor fica parada, o que atrasa as mensagens
  seguintes; o reprocessamento da DLQ é manual. Se o serviço cair depois de enviar o e-mail e antes do commit,
  ainda pode sair um e-mail duplicado (janela pequena, aceita na PoC).
- **Melhorias futuras:** filas de espera com TTL para não segurar a thread; reprocessamento automático
  agendado; *Transactional Outbox* no produtor (ver ADR-002).
