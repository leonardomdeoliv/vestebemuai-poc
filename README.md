# VesteBemUai – PoC de extração das notificações

Prova de conceito do Projeto Aplicado (pós-graduação em Arquitetura de Software e Soluções, XP Educação).
A VesteBemUai é uma loja online **fictícia** de roupas e acessórios. O objetivo é extrair o módulo de
notificações do monolítico para um microsserviço ligado por fila (RabbitMQ), com o padrão **Strangler Fig**,
para que o checkout deixe de depender do servidor de e-mail.

| Sprint | Período | Objetivo | Situação |
|---|---|---|---|
| 1 | 02/11 a 08/11/2026 | Fundação e estado atual: ambiente, monolítico simulado, linha de base, C4 atual, ADR-001 | Concluída |
| 2 | 09/11 a 15/11/2026 | Extração do serviço de notificações: evento, reenvio, DLQ, idempotência | Em andamento |
| 3 | 16/11 a 22/11/2026 | Validação (H1 a H4), evidência na AWS com Terraform e DAS final | A fazer |

## Como rodar

Pré-requisitos: Docker com Docker Compose. Java 21 e Maven só são necessários para rodar os testes fora do Docker.

```bash
docker compose up -d --build      # sobe PostgreSQL, RabbitMQ, Mailpit, o monolítico e o serviço de notificações
docker compose ps                 # todos devem estar Up/healthy
```

| Serviço | Endereço |
|---|---|
| API do monolítico | http://localhost:8080/pedidos |
| Serviço de notificações (DLQ) | http://localhost:8081/dlq |
| Saúde da API (Actuator) | http://localhost:8080/actuator/health |
| Caixa de e-mails (Mailpit) | http://localhost:8025 |
| Painel do RabbitMQ | http://localhost:15672 (usuário e senha `vestebemuai`) |

Criar um pedido:

```bash
curl -X POST http://localhost:8080/pedidos -H 'Content-Type: application/json' -d '{
  "clienteNome": "Ana Souza",
  "clienteEmail": "ana.souza@exemplo.com",
  "itens": [
    {"produto": "Vestido midi floral", "tamanho": "P", "cor": "Verde", "quantidade": 1, "precoUnitario": 189.90}
  ]
}'
```

Rodar os testes: `cd monolito && mvn test` e `cd notificacoes-service && mvn test`

Rodar o monolítico sem Docker (banco H2 em memória; o RabbitMQ precisa estar no ar na porta 5672):
`cd monolito && mvn spring-boot:run -Dspring-boot.run.profiles=h2`

## Linha de base (S1.5)

```bash
./scripts/linha-de-base.sh 100
```

Envia 100 pedidos com o Mailpit no ar e 100 com o Mailpit parado. O resultado vai para
`docs/sprints/sprint-1/evidencias/`. Resultado de 02/11/2026:

| Cenário | Enviados | Concluídos | Gravados no banco | Tempo médio |
|---|---|---|---|---|
| Mailpit no ar | 100 | 100 | 100 | 14 ms |
| Mailpit parado | 100 | **0** | **0** | 256 ms |

## Sprint 2: notificações pela fila

Desde a Sprint 2 o checkout grava o pedido e publica o evento `PedidoCriado` no RabbitMQ; o
`notificacoes-service` consome o evento e envia o e-mail, com 3 reenvios (2 s, 4 s e 8 s), DLQ e idempotência.

```bash
./scripts/falha-smtp-dlq.sh       # Mailpit parado: pedido 201, 4 tentativas, DLQ, reprocessamento e e-mail entregue
./scripts/mensagem-duplicada.sh   # mesma mensagem publicada 2 vezes gera 1 e-mail
curl -X POST http://localhost:8081/dlq/reprocessar   # devolve a DLQ para a fila principal
```

O modo antigo continua disponível para comparação (Sprint 3):
`NOTIFICACAO_MODO=sincrono docker compose up -d --no-deps monolito`.

## Documentação

- [Requisitos e critérios de aceite](docs/requisitos.md)
- [C4 nível 1 (contexto)](docs/c4/c4-contexto.png) e [nível 2 (contêineres)](docs/c4/c4-conteineres.png) do estado atual; fonte em [workspace.dsl](docs/c4/fontes/workspace.dsl)
- [C4 nível 2 da arquitetura alvo](docs/c4/c4-conteineres-alvo.png) e [nível 3 do serviço de notificações](docs/c4/c4-componentes-notificacoes.png); fonte em [workspace-alvo.dsl](docs/c4/fontes/workspace-alvo.dsl)
- [ADR-001: Strangler Fig para extrair as notificações](docs/adr/ADR-001-strangler-fig-notificacoes.md)
- [ADR-002: RabbitMQ como broker](docs/adr/ADR-002-rabbitmq-como-broker.md)
- [ADR-003: reenvio, DLQ e idempotência](docs/adr/ADR-003-reenvio-e-dlq.md)
- [ADR-004: evidência na AWS com Terraform](docs/adr/ADR-004-evidencia-aws-terraform.md)
- [Estudo dirigido de RabbitMQ](docs/estudo-rabbitmq.md)
- [Sprint 1: planejamento e evidências](docs/sprints/sprint-1/README.md)
- [Sprint 2: planejamento e evidências](docs/sprints/sprint-2/README.md)

## Estrutura

```
docker-compose.yml     ambiente completo (RNF04)
monolito/              monolítico simulado (Spring Boot)
notificacoes-service/  serviço de notificações extraído (Spring Boot)
scripts/               scripts de teste e medição
docs/                  requisitos, C4, ADRs e evidências de cada sprint
```

Todos os dados são fictícios.
