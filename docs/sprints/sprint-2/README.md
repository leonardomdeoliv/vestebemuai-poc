# Sprint 2 – Extração do serviço de notificações

- **Período planejado:** 09/11 a 15/11/2026 (entrega em 15/11)
- **Objetivo:** extrair o serviço de notificações do monolítico, ligá-lo pela fila (RabbitMQ) e implementar
  reenvio, DLQ e idempotência; desligar o envio síncrono no checkout (Strangler Fig, ADR-001).
- **Capacidade:** cerca de 15 horas na semana (premissa P6).

## 1. Evidência de planejamento

Quadro Kanban no [GitHub Projects](https://github.com/users/leonardomdeoliv/projects/1), com uma issue por task
([issues da Sprint 2](https://github.com/leonardomdeoliv/vestebemuai-poc/issues?q=label%3Asprint-2)).
O critério de conclusão de cada task foi escrito na issue **antes** da execução (ajuste vindo da Sprint 1).

| Task | Item | Requisito | Critério de conclusão | Estimativa | Dias planejados |
|---|---|---|---|---|---|
| S2.1 ([#9](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/9)) | C4 níveis 2 e 3 da arquitetura alvo; ADR-002, ADR-003 e ADR-004 | RNF05 | Diagramas exportados em PNG; ADRs com contexto, opções, decisão e consequências | 3 h | 09/11 a 10/11 |
| S2.2 ([#10](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/10)) | Publicar o evento PedidoCriado no monolítico | RF02 | Evento no exchange `pedidos` só depois do commit; mensagem visível no painel; teste JUnit | 3 h | 10/11 a 11/11 |
| S2.3 ([#11](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/11)) | Serviço de notificações que consome o evento e envia o e-mail | RF03, RF06 | Contêiner próprio; e-mail no Mailpit enviado pelo serviço; modelo do e-mail em arquivo próprio | 4 h | 10/11 a 12/11 |
| S2.4 ([#12](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/12)) | Reenvio 3 vezes com intervalo crescente e DLQ com reprocessamento | RF04, RF05 | Tentativas em 2 s, 4 s e 8 s no log; mensagem na `notificacoes.dlq`; reprocessamento entrega o e-mail | 4 h | 12/11 a 13/11 |
| S2.5 ([#13](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/13)) | Idempotência pelo identificador do evento | RNF02 | Mesma mensagem duas vezes gera um único e-mail; teste JUnit | 2 h | 13/11 a 14/11 |
| S2.6 ([#14](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/14)) | Desligar o envio síncrono no monolítico | RF02 | Flag `NOTIFICACAO_MODO` (fila/sincrono); com o Mailpit parado, `POST /pedidos` responde 201 | 2 h | 14/11 |
| S2.7 ([#15](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/15)) | Registrar evidências e lições aprendidas | — | Seção 2.2 do relatório no formato do professor | 2 h | 14/11 a 15/11 |

## 2. Evidências de execução (o processo)

| Task | Evidência | Legenda |
|---|---|---|
| S2.1 | [e1-workspace-alvo-dsl.png](evidencias/e1-workspace-alvo-dsl.png) | Modelo C4 da arquitetura alvo descrito em Structurizr DSL (contêineres e componentes do serviço) |
| S2.2 | [e2-publicador-pedido-criado.png](evidencias/e2-publicador-pedido-criado.png) | Publicação do evento depois do commit (`@TransactionalEventListener(AFTER_COMMIT)`) |
| S2.3 | [e3-pedido-criado-listener.png](evidencias/e3-pedido-criado-listener.png) | Consumidor `@RabbitListener` da fila `notificacoes.pedido-criado`, com log de cada tentativa |
| S2.4 | [e4-retry-dlq-config.png](evidencias/e4-retry-dlq-config.png) | Configuração do reenvio (4 tentativas, 2 s × 2 até 8 s) e da rejeição para a DLQ |
| S2.5 | [e5-idempotencia-service.png](evidencias/e5-idempotencia-service.png) | Verificação do `eventoId` na mesma transação do envio |
| S2.6 | [e6-checkout-service.png](evidencias/e6-checkout-service.png) | Checkout com a flag de modo: `FILA` publica o evento, `SINCRONO` mantém o envio antigo |
| Todas | [e7-testes-junit.png](evidencias/e7-testes-junit.png) | 13 testes JUnit passando (7 no monolítico, 6 no serviço) |

## 3. Evidências de resultado (o que foi entregue)

| Task | Resultado | Onde está |
|---|---|---|
| S2.1 | C4 nível 2 alvo e nível 3 do serviço; ADR-002, 003 e 004 aceitas | [c4-conteineres-alvo.png](../../c4/c4-conteineres-alvo.png), [c4-componentes-notificacoes.png](../../c4/c4-componentes-notificacoes.png), [ADR-002](../../adr/ADR-002-rabbitmq-como-broker.md), [ADR-003](../../adr/ADR-003-reenvio-e-dlq.md), [ADR-004](../../adr/ADR-004-evidencia-aws-terraform.md) |
| S2.2 | Pedido 201 e evento `PedidoCriado` na fila, com `message_id` = `eventoId` | [r2a](evidencias/r2a-post-pedido-publica-evento.png), [r2-rabbitmq-mensagem-pedido-criado.png](evidencias/r2-rabbitmq-mensagem-pedido-criado.png) |
| S2.3 | Serviço no Compose (5 contêineres) e e-mail entregue pelo serviço, inclusive do pedido feito com o serviço parado | [r3a](evidencias/r3a-docker-compose-ps.png), [r3b](evidencias/r3b-notificacoes-consumo.png), [r3-mailpit-email-servico.png](evidencias/r3-mailpit-email-servico.png), [notificacoes-service/](../../../notificacoes-service) |
| S2.4 | 4 tentativas (2 s, 4 s, 8 s), mensagem na DLQ, reprocessamento e e-mail entregue | [r4a](evidencias/r4a-falha-smtp-dlq.png), [r4-rabbitmq-dlq-com-mensagem.png](evidencias/r4-rabbitmq-dlq-com-mensagem.png), [scripts/falha-smtp-dlq.sh](../../../scripts/falha-smtp-dlq.sh) |
| S2.5 | 2 publicações da mesma mensagem, 1 e-mail | [r5a](evidencias/r5a-mensagem-duplicada.png), [r5-mailpit-busca-duplicada.png](evidencias/r5-mailpit-busca-duplicada.png), [scripts/mensagem-duplicada.sh](../../../scripts/mensagem-duplicada.sh) |
| S2.6 | Com o Mailpit parado: modo fila = HTTP 201 e pedido gravado; modo síncrono = HTTP 503 e nada gravado | [r6-modos-fila-x-sincrono.png](evidencias/r6-modos-fila-x-sincrono.png) |

### Rastreabilidade

| Requisito | Hipótese | Código | Teste ou evidência |
|---|---|---|---|
| RF02 | H1 | `PublicadorPedidoCriado`, `CheckoutService` | `CheckoutFilaTest`, r2a, r2 |
| RF03 | H1 | `PedidoCriadoListener`, `EnvioEmailService` | `NotificacaoPedidoTest.enviaEmailDeConfirmacao`, r3 |
| RF04 | H2 | `application.yml` (retry), `PedidoCriadoListener` | `TopologiaRabbitTest.reenvioComIntervaloCrescente`, r4a |
| RF05 | H2 | `RabbitConfig` (DLX/DLQ), `DlqController` | `TopologiaRabbitTest.filaPrincipalTemDeadLetter`, r4a, r4 |
| RF06 | H3 | `ModeloEmail`, `templates/email-confirmacao.html` | Validação na Sprint 3 (trocar o modelo publicando só o serviço) |
| RNF02 | H2 | `NotificacaoPedidoService`, `EventoProcessado` | `NotificacaoPedidoTest.mensagemDuplicadaGeraUmEmail`, r5a, r5 |
| RNF01 | H1 | `CheckoutService` (modo fila) | `CheckoutFilaTest.concluiSemDependerDoSmtp`, r6; 100 pedidos na Sprint 3 |

## 4. Lições aprendidas

Ver a seção 2.2.2 do relatório.
