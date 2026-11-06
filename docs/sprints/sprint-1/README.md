# Sprint 1 – Fundação e estado atual

- **Período planejado:** 02/11 a 08/11/2026 (entrega em 08/11)
- **Execução:** iniciada antecipadamente em 28/10/2026
- **Objetivo:** montar o ambiente, construir o monolítico simulado, medir o problema atual (linha de base)
  e documentar o estado atual com C4 e a ADR-001.
- **Capacidade:** cerca de 15 horas na semana (premissa P6).

## 1. Evidência de planejamento

Quadro Kanban no [GitHub Projects](https://github.com/users/leonardomdeoliv/projects/1), com uma issue por task
([issues da Sprint 1](https://github.com/leonardomdeoliv/vestebemuai-poc/milestone/1?closed=1)).

| Task | Item | Requisito | Critério de aceite | Estimativa | Dias planejados | Situação |
|---|---|---|---|---|---|---|
| S1.1 | Detalhar requisitos e critérios de aceite | — | Todos os RF e RNF com critério verificável e sprint de validação | 2 h | 02/11 a 03/11 | Feito |
| S1.2 | Repositório no GitHub e Docker Compose (PostgreSQL, RabbitMQ, Mailpit); alerta de orçamento na AWS | RNF04 | `docker compose up -d` sobe tudo sem passo manual; alerta de US$ 5 no AWS Budgets | 3 h | 02/11 a 03/11 | Feito |
| S1.3 | Monolítico simulado com checkout e e-mail síncrono | RF01 | `POST /pedidos` grava o pedido com itens (produto, tamanho, cor) e envia o e-mail; entrada inválida devolve 400 | 5 h | 04/11 a 06/11 | Feito |
| S1.4 | C4 níveis 1 e 2 do estado atual | RNF05 | Diagramas de contexto e contêineres mostrando a dependência síncrona do SMTP | 2 h | 05/11 a 07/11 | Feito |
| S1.5 | Reproduzir o problema com o Mailpit parado (linha de base) | RNF01 | Script com 100 pedidos em cada cenário, resultado gravado em CSV | 2 h | 06/11 a 07/11 | Feito |
| S1.6 | ADR-001: Strangler Fig para extrair as notificações | RNF05 | ADR com contexto, opções, decisão e consequências | 1 h | 07/11 a 08/11 | Feito |
| Apoio | Estudo dirigido de RabbitMQ (premissa P5) | — | Resumo dos conceitos e uma mensagem publicada e lida no painel | 2 h | toda a sprint | Feito |

## 2. Evidências de execução (o processo)

| Task | Evidência | Legenda |
|---|---|---|
| S1.1 | Commit com `docs/requisitos.md` | Requisitos escritos no formato Dado/Quando/Então, cada um com a forma de medição |
| S1.2 | `evidencias/logs/docker-compose-up.txt` | `docker compose up -d` criando a rede, o volume e os quatro contêineres, esperando o PostgreSQL e o RabbitMQ ficarem saudáveis |
| S1.3 | [CheckoutService.java](../../../monolito/src/main/java/br/com/vestebemuai/monolito/pedido/CheckoutService.java), `07-testes-junit.png` | Checkout grava o pedido e chama o envio do e-mail na mesma transação; testes JUnit rodando |
| S1.4 | [workspace.dsl](../../c4/fontes/workspace.dsl) | Modelo C4 descrito em Structurizr DSL (pessoas, sistemas, contêineres e relações) |
| S1.5 | `09-linha-de-base-execucao.png` | Script enviando 100 pedidos com o Mailpit no ar e 100 com o Mailpit parado |
| S1.6 | Commit com a ADR-001 | Registro das três opções consideradas e da decisão |
| Apoio | `04-rabbitmq-fila-estudo.png` | Fila de estudo criada e mensagem publicada pelo painel do RabbitMQ |

## 3. Evidências de resultado (o que foi entregue)

| Task | Resultado | Link |
|---|---|---|
| S1.1 | Documento de requisitos e critérios de aceite | [docs/requisitos.md](../../requisitos.md) |
| S1.2 | Ambiente com os quatro serviços no ar (`01-docker-compose-ps.png`, `03-rabbitmq-painel.png`) e orçamento de US$ 5 no AWS Budgets (`r2-aws-budget.png`) | [docker-compose.yml](../../../docker-compose.yml) |
| S1.3 | Pedido criado (HTTP 201) e e-mail de confirmação no Mailpit (`08a-post-pedido-201.png`, `06-mailpit-email-confirmacao.png`) | [monolito/](../../../monolito) |
| S1.4 | Diagramas C4 de contexto e de contêineres | [c4-contexto.png](../../c4/c4-contexto.png), [c4-conteineres.png](../../c4/c4-conteineres.png) |
| S1.5 | Linha de base: 0 de 100 pedidos concluídos com o Mailpit parado (`10-grafico-linha-de-base.png`) | [resumo.md](evidencias/resumo.md), [CSV controle](evidencias/linha-de-base-controle.csv), [CSV falha](evidencias/linha-de-base-falha.csv) |
| S1.6 | ADR-001 aceita | [ADR-001](../../adr/ADR-001-strangler-fig-notificacoes.md) |
| Apoio | Resumo de RabbitMQ para a Sprint 2 | [estudo-rabbitmq.md](../../estudo-rabbitmq.md) |

### Linha de base

| Cenário | Enviados | Concluídos (HTTP 201) | Gravados no banco | Tempo médio |
|---|---|---|---|---|
| Mailpit no ar | 100 | 100 (100%) | 100 | 14 ms |
| Mailpit parado | 100 | **0 (0%)** | **0** | 256 ms |

A premissa P1 se confirmou: o monolítico simulado reproduz o problema. Com o servidor de e-mail fora do ar,
nenhum pedido é concluído, porque a falha no SMTP desfaz a transação do pedido.

## 4. Lições aprendidas

Ver a seção 2.1.2 do relatório.
