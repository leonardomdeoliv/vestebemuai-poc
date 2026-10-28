# Cartões do Trello – Sprint 1

Quadro: **VesteBemUai – PoC** · Listas: A fazer | Fazendo | Feito
Etiquetas: Doc (azul), Infra (roxo), Código (verde), Teste (laranja), Sprint 1 (cinza)

1. **S1.1 Detalhar requisitos e critérios de aceite** · Doc · entrega 03/11
   - [ ] Critério Dado/Quando/Então para RF01 a RF06
   - [ ] Critério e forma de medir para RNF01 a RNF06
   - [ ] Publicar docs/requisitos.md no GitHub
2. **S1.2 Repositório, Docker Compose e alerta AWS** · Infra · RNF04 · entrega 03/11
   - [ ] Criar repositório vestebemuai-poc no GitHub
   - [ ] docker-compose.yml com PostgreSQL, RabbitMQ e Mailpit
   - [ ] `docker compose up -d` sobe tudo (print do `docker compose ps`)
   - [ ] Alerta de orçamento de US$ 5 no AWS Budgets (print)
3. **S1.3 Monolítico simulado com checkout e e-mail síncrono** · Código · RF01 · entrega 06/11
   - [ ] POST /pedidos com itens (produto, tamanho, cor)
   - [ ] Envio do e-mail na mesma transação
   - [ ] Testes JUnit passando (print)
   - [ ] E-mail de confirmação no Mailpit (print)
4. **S1.4 C4 níveis 1 e 2 do estado atual** · Doc · RNF05 · entrega 07/11
   - [ ] Diagrama de contexto
   - [ ] Diagrama de contêineres
5. **S1.5 Linha de base com o Mailpit parado** · Teste · RNF01 · entrega 07/11
   - [ ] Script com 100 pedidos (controle e falha)
   - [ ] CSV e gráfico do resultado
6. **S1.6 ADR-001 Strangler Fig** · Doc · RNF05 · entrega 08/11
   - [ ] Contexto, opções, decisão e consequências
7. **Estudo dirigido de RabbitMQ (P5)** · Doc · entrega 08/11
   - [ ] Resumo dos conceitos (exchange, fila, binding, ack, DLQ)
   - [ ] Publicar e ler uma mensagem no painel
8. **Retrospectiva da Sprint 1** · Doc · entrega 08/11
