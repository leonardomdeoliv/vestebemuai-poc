# ADR-001: Usar o padrão Strangler Fig para extrair as notificações

- **Status:** Aceita
- **Data:** 28/10/2026
- **Decisores:** Leonardo (arquiteto e desenvolvedor do projeto)

## Contexto

O checkout do monolítico da VesteBemUai grava o pedido e envia o e-mail de confirmação na mesma
transação e na mesma thread (ver `CheckoutService`). A linha de base da Sprint 1 mostrou o efeito:
com o servidor de e-mail parado, **0 de 100 pedidos** foram concluídos, porque a falha no SMTP faz
rollback do pedido. Com o e-mail no ar, os 100 pedidos foram concluídos.

A empresa não pode parar a loja para reescrever o sistema, e a equipe é pequena. Precisamos remover
esse acoplamento sem uma migração "big bang".

## Opções consideradas

1. **Reescrever o monolítico como microsserviços de uma vez (big bang).** Alto risco, não cabe em 3 sprints
   e deixa a loja sem evolução durante a reescrita.
2. **Manter o monolítico e só tornar o envio assíncrono dentro dele** (`@Async` ou fila em memória).
   Resolve parte da latência, mas o e-mail continua no mesmo processo e no mesmo deploy, e mensagens em memória
   se perdem se a aplicação reiniciar.
3. **Strangler Fig:** extrair só o módulo de notificações para um serviço novo, ligado por uma fila (evento
   `PedidoCriado`), e desligar o código antigo quando o novo estiver validado.

## Decisão

Adotar a opção 3. O monolítico passa a publicar o evento `PedidoCriado` e um novo serviço de notificações
consome o evento e envia o e-mail. A troca é feita em passos que podem ser revertidos:

1. Publicar o evento mantendo o envio síncrono (os dois convivem).
2. Ligar o serviço de notificações e conferir os e-mails no Mailpit.
3. Desligar o envio síncrono no monolítico (S2.6): o módulo antigo é "estrangulado".

## Consequências

- **Positivas:** o checkout deixa de depender do SMTP (H1); o serviço pode ter reenvio e DLQ (H2) e ser publicado
  sozinho (H3); o mesmo caminho serve para extrair pedidos, estoque e pagamentos no futuro (H5).
- **Negativas:** mais uma peça para operar (RabbitMQ); consistência eventual (o e-mail chega alguns segundos depois);
  risco de e-mail duplicado, tratado com idempotência pelo id do evento (RNF02).
- **Escolha do broker:** fica para a ADR-002 (RabbitMQ), na Sprint 2.
