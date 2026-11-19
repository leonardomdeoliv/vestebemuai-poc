# Sprint 3 – Validação e DAS

- **Período planejado:** 16/11 a 22/11/2026 (entrega em 23/11)
- **Objetivo:** validar as hipóteses H1 a H4 com medições comparáveis à linha de base da Sprint 1, repetir o
  teste de falha em uma EC2 na AWS criada e destruída com Terraform (ADR-004) e consolidar o DAS final.
- **Capacidade:** cerca de 15 horas na semana (premissa P6).

## 1. Evidência de planejamento

Quadro Kanban no [GitHub Projects](https://github.com/users/leonardomdeoliv/projects/1), com uma issue por task
([issues da Sprint 3](https://github.com/leonardomdeoliv/vestebemuai-poc/issues?q=label%3Asprint-3)) e o
critério de conclusão escrito na issue antes da execução, todas na milestone
[Sprint 3](https://github.com/leonardomdeoliv/vestebemuai-poc/milestone/3). Prints: quadro no início
([p-quadro-github-projects.png](evidencias/p-quadro-github-projects.png)) e ao final, com S3.1 a S3.6 em Done
([p-quadro-done.png](evidencias/p-quadro-done.png)).

| Task | Item | Requisito | Critério de conclusão | Estimativa | Dias planejados |
|---|---|---|---|---|---|
| S3.1 ([#17](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/17)) | Observabilidade: Actuator, logs e painel do RabbitMQ | RNF03 | Métricas de envio, falha, DLQ e duplicados no Actuator; filas visíveis no painel | 2 h | 16/11 a 17/11 |
| S3.2 ([#18](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/18)) | Teste de falha: 100 pedidos com o Mailpit parado | RNF01 | Modo fila 100/100 com HTTP 201 (linha de base: 0/100) | 2 h | 16/11 a 17/11 |
| S3.3 ([#19](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/19)) | Entrega após o retorno do e-mail | RF04, RF05, RNF02 | ≥ 99% dos pedidos com e-mail e nenhum duplicado | 2 h | 17/11 a 18/11 |
| S3.4 ([#20](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/20)) | Publicar só o serviço e medir o tempo de deploy | RF06 | Modelo novo no e-mail; monolítico sem reinício e respondendo 201 durante a troca | 2 h | 18/11 |
| S3.5 ([#21](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/21)) | EC2 na AWS com Terraform, teste de falha e destroy | RNF06 | apply, teste na nuvem, destroy no mesmo dia, custo < US$ 5 | 3 h | 19/11 a 20/11 |
| S3.6 ([#22](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/22)) | DAS final com C4 e ADRs | RNF05 | [docs/DAS.md](../../DAS.md) com visões, decisões, resultados e roteiro de reuso | 3 h | 20/11 a 21/11 |
| S3.7 ([#23](https://github.com/leonardomdeoliv/vestebemuai-poc/issues/23)) | Evidências, retrospectiva e lições aprendidas | — | Seção 2.3 do relatório | 1 h | 21/11 a 22/11 |

## 2. Evidências de execução (o processo)

| Task | Evidência | Legenda |
|---|---|---|
| S3.1 | [e1-metricas-filas.png](evidencias/e1-metricas-filas.png) | Medidores da fila principal e da DLQ publicados no Actuator |
| S3.2 | [e2-teste-falha-h1.png](evidencias/e2-teste-falha-h1.png) | Script que roda 100 pedidos com o Mailpit parado nos modos síncrono e fila |
| S3.3 | [e3-entrega-h2.png](evidencias/e3-entrega-h2.png) | Script que espera a DLQ, religa o Mailpit, reprocessa e conta e-mails por pedido |
| S3.4 | [e4-deploy-notificacoes.png](evidencias/e4-deploy-notificacoes.png) | Script que muda o modelo e republica só o serviço enquanto chegam pedidos |
| S3.5 | [e5-terraform-main.png](evidencias/e5-terraform-main.png) | Terraform: security group só para o IP do autor e EC2 t3.medium com *user data* |
| S3.6 | [e6-das-fonte.png](evidencias/e6-das-fonte.png) | DAS escrito em Markdown no repositório |
| Todas | [e7-testes-junit.png](evidencias/e7-testes-junit.png) | 17 testes JUnit passando (7 no monolítico, 10 no serviço) |

## 3. Evidências de resultado

| Task | Resultado | Onde está |
|---|---|---|
| S3.1 | 400 falhas, 100 mensagens na DLQ e o tamanho da DLQ visíveis no Actuator; filas no painel | [r1-actuator-metricas.png](evidencias/r1-actuator-metricas.png), [r1-rabbitmq-filas-em-reenvio.png](evidencias/r1-rabbitmq-filas-em-reenvio.png) |
| S3.2 | Mailpit parado: **síncrono 0/100 (HTTP 503)** e **fila 100/100 (HTTP 201)** | [r2-teste-falha-h1.png](evidencias/r2-teste-falha-h1.png), [r2-grafico-h1.png](evidencias/r2-grafico-h1.png), [local/resumo-h1.md](evidencias/local/resumo-h1.md) |
| S3.3 | 100 na DLQ → reprocessadas → **100% entregues, 0 duplicados**, filas vazias | [r3-entrega-h2.png](evidencias/r3-entrega-h2.png), [r3-rabbitmq-dlq-100.png](evidencias/r3-rabbitmq-dlq-100.png), [r3-mailpit-100-emails.png](evidencias/r3-mailpit-100-emails.png), [local/resumo-h2.md](evidencias/local/resumo-h2.md) |
| S3.4 | Só o serviço republicado em **14,7 s**; 38/38 pedidos com 201 durante a troca; monolítico sem reinício | [r4-deploy-notificacoes.png](evidencias/r4-deploy-notificacoes.png), [r4-mailpit-email-modelo-novo.png](evidencias/r4-mailpit-email-modelo-novo.png), [local/resumo-h3.md](evidencias/local/resumo-h3.md) |
| S3.5 | EC2 t3.medium criada às 19:32 de 19/11, ambiente pronto em 126 s; H1 **100/100**, H2 **100%** sem duplicados, H3 **30,6 s** com 66/66 pedidos; destroy às 20:01 (5 recursos, instância *terminated*); custo estimado ≈ US$ 0,02 | [r5a apply](evidencias/r5a-terraform-apply.png), [r5b console](evidencias/r5b-aws-console-ec2.png), [r5c](evidencias/r5c-aws-ec2-pronta.png), [r5d H1](evidencias/r5d-aws-teste-falha-h1.png), [r5e H2](evidencias/r5e-aws-entrega-h2.png), [r5f H3](evidencias/r5f-aws-deploy-notificacoes.png), [r5g métricas](evidencias/r5g-aws-metricas.png), [DLQ](evidencias/r5-aws-rabbitmq-dlq-100.png), [Mailpit](evidencias/r5-aws-mailpit-100-emails.png), [e-mail novo](evidencias/r5-aws-mailpit-email-frete.png), [r5h destroy](evidencias/r5h-terraform-destroy.png), [logs](evidencias/aws) |
| S3.6 | DAS final | [docs/DAS.md](../../DAS.md), [r6-das.png](evidencias/r6-das.png), [c4-implantacao.png](../../c4/c4-implantacao.png) |

## 4. Como reproduzir

```bash
docker compose up -d --build
./scripts/teste-falha-h1.sh 100        # S3.2: deixa o Mailpit parado no final
./scripts/entrega-h2.sh 100            # S3.3: continua do ponto anterior
./scripts/deploy-notificacoes.sh "Novo aviso no e-mail"   # S3.4
cd infra/terraform && terraform apply -var branch=main     # S3.5 (precisa de credenciais AWS)
```

## 5. Lições aprendidas

Ver a seção 2.3.2 do relatório.
