#!/usr/bin/env bash
# S2.5 - Idempotencia (RNF02 / hipotese H2)
#
# Publica a MESMA mensagem PedidoCriado (mesmo eventoId) duas vezes no exchange
# "pedidos", pela API HTTP do RabbitMQ, e confere quantos e-mails chegaram no Mailpit.
# Esperado: 2 mensagens publicadas, 1 e-mail entregue.
#
# Uso: ./scripts/mensagem-duplicada.sh
set -euo pipefail

RABBIT=${RABBIT:-http://localhost:15672}
MAILPIT=${MAILPIT:-http://localhost:8025}
USUARIO=${RABBIT_USER:-vestebemuai}:${RABBIT_PASS:-vestebemuai}

uuid() { python3 -c 'import uuid; print(uuid.uuid4())'; }
EVENTO=$(uuid)
PEDIDO=$(uuid)
EMAIL="duplicada-${EVENTO:0:8}@exemplo.com"
PAYLOAD=$(printf '{"eventoId":"%s","pedidoId":"%s","clienteNome":"Cliente Duplicada","clienteEmail":"%s","itens":[{"produto":"Moletom canguru","tamanho":"G","cor":"Cinza","quantidade":1,"precoUnitario":149.90}],"total":149.90,"criadoEm":"2026-11-13T10:00:00-03:00"}' \
  "$EVENTO" "$PEDIDO" "$EMAIL")
CORPO=$(python3 -c 'import json,sys; print(json.dumps({"properties":{"content_type":"application/json","message_id":sys.argv[1]},"routing_key":"pedido.criado","payload":sys.argv[2],"payload_encoding":"string"}))' "$EVENTO" "$PAYLOAD")

echo ">> eventoId: $EVENTO  destinatario: $EMAIL"
for i in 1 2; do
  echo -n ">> publicacao $i: "
  curl -s -u "$USUARIO" -H 'content-type: application/json' -X POST "$RABBIT/api/exchanges/%2F/pedidos/publish" -d "$CORPO"
  echo
done

sleep 3
TOTAL=$(curl -s "$MAILPIT/api/v1/search?query=to:$EMAIL" | python3 -c 'import json,sys; print(json.load(sys.stdin)["messages_count"])')
echo ">> mensagens publicadas: 2 | e-mails entregues para $EMAIL: $TOTAL"
[ "$TOTAL" = "1" ] && echo ">> OK: a mensagem duplicada foi ignorada (RNF02)" || { echo ">> FALHA: esperado 1 e-mail"; exit 1; }
