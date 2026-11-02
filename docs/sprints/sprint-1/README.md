# Sprint 1 – Fundação e estado atual

- **Período planejado:** 02/11 a 08/11/2026 (entrega em 08/11)
- **Execução:** iniciada antecipadamente em 28/10/2026
- **Objetivo:** montar o ambiente, construir o monolítico simulado, medir o problema atual (linha de base)
  e documentar o estado atual com C4 e a ADR-001.
- **Capacidade:** cerca de 15 horas na semana (premissa P6).

## Backlog da sprint (planejamento)

| ID | Item | Requisito | Critério de aceite | Estimativa | Dias planejados | Situação | Evidência |
|---|---|---|---|---|---|---|---|
| S1.1 | Detalhar requisitos e critérios de aceite | — | Todos os RF e RNF com critério verificável e sprint de validação | 2 h | 02/11 a 03/11 | Feito | [requisitos.md](../../requisitos.md) |
| S1.2 | Repositório no GitHub e Docker Compose (PostgreSQL, RabbitMQ, Mailpit); conta AWS com alerta de orçamento | RNF04 | `docker compose up -d` sobe tudo sem passo manual; alerta de US$ 5 criado no AWS Budgets | 3 h | 02/11 a 03/11 | Feito (Compose e repositório); alerta AWS pendente | `01-docker-compose-ps.png`, `02-actuator-health.png`, `03-rabbitmq-painel.png` |
| S1.3 | Monolítico simulado com checkout e e-mail síncrono | RF01 | `POST /pedidos` grava o pedido com itens (produto, tamanho, cor) e envia o e-mail; entrada inválida devolve 400 | 5 h | 04/11 a 06/11 | Feito | `07-testes-junit.png`, `08a-post-pedido-201.png`, `06-mailpit-email-confirmacao.png` |
| S1.4 | C4 níveis 1 e 2 do estado atual | RNF05 | Diagramas de contexto e contêineres mostrando a dependência síncrona do SMTP | 2 h | 05/11 a 07/11 | Feito | [c4-contexto.png](../../c4/c4-contexto.png), [c4-conteineres.png](../../c4/c4-conteineres.png) |
| S1.5 | Reproduzir o problema com o Mailpit parado (linha de base) | RNF01 | Script com 100 pedidos em cada cenário, resultado gravado em CSV | 2 h | 06/11 a 07/11 | Feito | `08b-post-pedido-503.png`, `09-linha-de-base-execucao.png`, `10-grafico-linha-de-base.png` |
| S1.6 | ADR-001: Strangler Fig para extrair as notificações | RNF05 | ADR com contexto, opções, decisão e consequências | 1 h | 07/11 a 08/11 | Feito | [ADR-001](../../adr/ADR-001-strangler-fig-notificacoes.md) |
| — | Estudo dirigido de RabbitMQ (premissa P5) | — | Resumo dos conceitos e uma mensagem publicada e lida no painel | 2 h | ao longo da sprint | Feito | [estudo-rabbitmq.md](../../estudo-rabbitmq.md), `04-rabbitmq-fila-estudo.png` |

Total estimado: 17 h.

## Quadro Kanban (Trello)

Listas: **A fazer**, **Fazendo**, **Feito**. Um cartão por item, com a etiqueta do tipo (Doc, Infra, Código, Teste),
a data de entrega e um checklist com o critério de aceite. Os cartões estão em `trello-cartoes.md`.

## Resultado (linha de base)

| Cenário | Enviados | Concluídos (HTTP 201) | Gravados no banco | Tempo médio |
|---|---|---|---|---|
| Mailpit no ar | 100 | 100 (100%) | 100 | 14 ms |
| Mailpit parado | 100 | **0 (0%)** | **0** | 256 ms |

A premissa P1 se confirmou: o monolítico simulado reproduz o problema. Com o servidor de e-mail fora do ar,
nenhum pedido é concluído, porque a falha no SMTP desfaz a transação do pedido. Esse é o ponto de partida
do indicador "Pedidos concluídos com o e-mail fora do ar" (meta de 100% na Sprint 3).

Arquivos: `evidencias/resumo.md`, `evidencias/linha-de-base-controle.csv`, `evidencias/linha-de-base-falha.csv`.
