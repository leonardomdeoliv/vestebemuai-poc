#!/usr/bin/env bash
# S2.4 e S2.6 - Reenvio, DLQ e reprocessamento (RF04, RF05 / hipoteses H1 e H2)
#
# 1. Para o Mailpit e cria um pedido: o checkout responde 201 mesmo assim (S2.6).
# 2. O servico de notificacoes tenta 4 vezes (1 envio + 3 reenvios: 2 s, 4 s, 8 s)
#    e manda a mensagem para a DLQ (S2.4).
# 3. Sobe o Mailpit de novo, reprocessa a DLQ e confere o e-mail na caixa.
#
# Uso: ./scripts/falha-smtp-dlq.sh
set -euo pipefail

API=${API:-http://localhost:8080}
NOTIF=${NOTIF:-http://localhost:8081}
MAILPIT=${MAILPIT:-http://localhost:8025}
cd "$(dirname "$0")/.."

EMAIL="dlq-$(date +%H%M%S)@exemplo.com"
PEDIDO=$(printf '{"clienteNome":"Cliente DLQ","clienteEmail":"%s","itens":[{"produto":"Vestido midi floral","tamanho":"P","cor":"Verde","quantidade":1,"precoUnitario":189.90}]}' "$EMAIL")

echo ">> 1. Mailpit parado"
docker compose stop mailpit > /dev/null 2>&1

echo -n ">> 2. POST /pedidos com o SMTP fora do ar: "
curl -s -o /dev/null -w 'HTTP %{http_code} em %{time_total}s\n' -X POST "$API/pedidos" \
  -H 'Content-Type: application/json' -d "$PEDIDO"

echo ">> 3. Aguardando as 4 tentativas do servico de notificacoes (cerca de 40 s)..."
for _ in $(seq 1 60); do
  n=$(curl -s "$NOTIF/dlq" | python3 -c 'import json,sys; print(json.load(sys.stdin)["mensagens"])')
  [ "$n" -ge 1 ] && break; sleep 2
done
docker compose logs notificacoes 2>&1 | grep -E "Tentativa [0-9] de 4" | tail -4 | sed -E 's/^.*PedidoCriadoListener +: //'
echo ">> 4. Mensagens na DLQ: $(curl -s "$NOTIF/dlq")"

echo ">> 5. Mailpit de volta e reprocessamento da DLQ"
docker compose start mailpit > /dev/null 2>&1
for _ in $(seq 1 30); do curl -sf "$MAILPIT/livez" > /dev/null 2>&1 && break; sleep 1; done
# A JVM guarda por ~10 s a falha de DNS do host "mailpit"; esperar evita gastar as tentativas a toa.
sleep 10
curl -s -X POST "$NOTIF/dlq/reprocessar"; echo

contar() { curl -s "$MAILPIT/api/v1/search?query=to:$EMAIL" | python3 -c 'import json,sys; print(json.load(sys.stdin)["messages_count"])'; }
for _ in $(seq 1 30); do TOTAL=$(contar); [ "$TOTAL" -ge 1 ] && break; sleep 1; done
echo ">> 6. E-mails entregues para $EMAIL: $TOTAL | DLQ: $(curl -s "$NOTIF/dlq")"
[ "$TOTAL" = "1" ] && echo ">> OK: nenhum pedido perdido e o e-mail saiu depois do reprocessamento" || { echo ">> FALHA"; exit 1; }
