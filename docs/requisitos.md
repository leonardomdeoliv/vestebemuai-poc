# Requisitos e critérios de aceite (S1.1)

Detalhamento dos requisitos da Tabela 6 do relatório. Cada critério está no formato
**Dado / Quando / Então** e diz qual teste ou evidência vai comprová-lo e em que sprint.

## Requisitos funcionais

### RF01 – Registrar pedido com itens (Sprint 1)
- **Dado** um cliente com nome e e-mail válidos e ao menos um item com produto, tamanho, cor, quantidade (≥ 1) e preço (> 0),
  **quando** ele chama `POST /pedidos`, **então** a API responde `201 Created`, devolve o pedido com status `CONFIRMADO`
  e o total calculado, e o pedido fica gravado no PostgreSQL.
- **Dado** um pedido sem itens ou com e-mail inválido, **quando** ele é enviado, **então** a API responde `400` e nada é gravado.
- Evidência: `CheckoutTest` (JUnit) e chamada real à API com captura do Mailpit.

### RF02 – Publicar o evento PedidoCriado (Sprint 2)
- **Dado** um pedido concluído, **quando** a transação termina, **então** um evento `PedidoCriado` (id do evento, id do pedido,
  cliente, itens, total, data) é publicado no exchange do RabbitMQ, **e** a resposta do checkout não espera o envio do e-mail.
- Evidência: mensagem visível no painel do RabbitMQ; tempo do checkout igual com o Mailpit no ar ou parado.

### RF03 – Consumir o evento e enviar o e-mail (Sprint 2)
- **Dado** um evento `PedidoCriado` na fila, **quando** o serviço de notificações o consome, **então** um e-mail com o número do
  pedido, os itens e o total chega à caixa do Mailpit.

### RF04 – Reenvio com intervalo crescente (Sprint 2)
- **Dado** que o SMTP está fora do ar, **quando** o envio falha, **então** o serviço tenta de novo até 3 vezes com intervalo
  crescente (por exemplo 2 s, 4 s e 8 s), registrando cada tentativa no log.

### RF05 – DLQ e reprocessamento (Sprint 2)
- **Dado** que as 3 tentativas falharam, **quando** a última termina, **então** a mensagem vai para a fila `notificacoes.dlq`
  **e** pode ser devolvida para a fila principal (reprocessamento) sem perder dados.

### RF06 – Alterar o modelo do e-mail publicando só o serviço (Sprint 2/3)
- **Dado** um novo texto no modelo do e-mail, **quando** só o serviço de notificações é reconstruído e reiniciado,
  **então** os próximos e-mails saem com o novo modelo **e** o monolítico continua recebendo pedidos sem parar.

## Requisitos não funcionais

| ID | Critério de aceite | Como medir | Sprint |
|---|---|---|---|
| RNF01 | Com o Mailpit parado, 100% de 100 pedidos são concluídos (HTTP 201 e gravados). **Linha de base na Sprint 1.** | `scripts/linha-de-base.sh` | 1 (base) e 3 (meta) |
| RNF02 | Após o retorno do SMTP, cada pedido gera exatamente um e-mail; nenhum duplicado mesmo com reentrega da mensagem. | Comparar pedidos no banco com e-mails no Mailpit | 3 |
| RNF03 | Logs de cada tentativa e métricas do Actuator; filas e DLQ visíveis no painel do RabbitMQ. | Capturas de tela | 3 |
| RNF04 | `docker compose up -d` sobe PostgreSQL, RabbitMQ, Mailpit e as aplicações, sem passo manual. | `docker compose ps` com tudo `healthy`/`Up` | 1 |
| RNF05 | DAS com C4 níveis 1 a 3 (estado atual e alvo) e ADRs 001 a 004 no repositório. | Revisão do `docs/` | 1 a 3 |
| RNF06 | `terraform apply` cria a EC2, o teste de falha é repetido, `terraform destroy` remove tudo; custo ≤ US$ 5. | Saída do Terraform e painel de custos | 3 |
