#!/usr/bin/env bash
# S3.3 - Entrega depois que o e-mail volta (RF04, RF05, RNF02 / hipotese H2)
#
# Continua o teste-falha-h1.sh: os 100 eventos do modo fila esgotam as 4 tentativas
# e vao para a DLQ. Depois o Mailpit volta, a DLQ e reprocessada e o script conta
# os e-mails por pedido. Criterio: 99% ou mais entregues e nenhum duplicado.
#
# Uso: ./scripts/entrega-h2.sh [quantidade] [pasta de saida]
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/lib/comum.sh

N=${1:-100}
SAIDA=${2:-docs/sprints/sprint-3/evidencias}
PREFIXO=h1-fila
mkdir -p "$SAIDA"

echo ">> 1. Aguardando os reenvios (2 s, 4 s, 8 s) levarem os eventos para a DLQ"
inicio=$(agora_ms)
for _ in $(seq 1 300); do
  read -r prontas proc < <(fila_info "$FILA"); dlq=$(dlq_atual)
  [ "$prontas" = 0 ] && [ "$proc" = 0 ] && [ "$dlq" -ge "$N" ] && break
  [ "$prontas" = 0 ] && [ "$proc" = 0 ] && [ $(( $(agora_ms) - inicio )) -gt 60000 ] && break
  sleep 2
done
echo "   fila principal: $prontas prontas, $proc em reenvio | DLQ: $dlq mensagens ($(( ($(agora_ms) - inicio) / 1000 )) s)"
echo "   metricas: falhas=$(metrica "$NOTIF" notificacoes.emails.falhas) para_dlq=$(metrica "$NOTIF" notificacoes.mensagens.dlq) enviados=$(metrica "$NOTIF" notificacoes.emails.enviados)"
DLQ_ANTES=$dlq

echo ">> 2. Mailpit de volta"
docker compose start mailpit > /dev/null 2>&1; esperar_url "$MAILPIT/livez" 30
# A JVM guarda por ~10 s a falha de DNS do host "mailpit"; esperar evita gastar as tentativas a toa.
sleep 12

echo ">> 3. Reprocessando a DLQ: $(curl -s -X POST "$NOTIF/dlq/reprocessar")"
t0=$(agora_ms)
for _ in $(seq 1 180); do
  read -r prontas proc < <(fila_info "$FILA")
  read -r total unicos dup < <(emails_por_prefixo "$PREFIXO")
  [ "$prontas" = 0 ] && [ "$proc" = 0 ] && [ "$unicos" -ge "$N" ] && break; sleep 1
done
seg=$(awk "BEGIN{printf \"%.1f\", ($(agora_ms) - $t0)/1000}")
sleep 3 # margem para algum e-mail duplicado aparecer
read -r total unicos dup < <(emails_por_prefixo "$PREFIXO")
dlq=$(dlq_atual)
taxa=$(awk "BEGIN{printf \"%.1f\", $unicos*100/$N}")
echo "   e-mails no Mailpit: total=$total pedidos_com_email=$unicos duplicados=$dup em ${seg}s | DLQ=$dlq"
echo "   metricas: enviados=$(metrica "$NOTIF" notificacoes.emails.enviados) duplicados_ignorados=$(metrica "$NOTIF" notificacoes.eventos.duplicados) reprocessadas=$(metrica "$NOTIF" notificacoes.dlq.reprocessadas)"

{
  echo "# S3.3 - Entrega apos o retorno do e-mail (H2)"
  echo
  echo "Executado em $(date '+%d/%m/%Y %H:%M') em $(hostname)."
  echo
  echo "| Pedidos no modo fila | Na DLQ antes | E-mails entregues | Pedidos com e-mail | Duplicados | Taxa de entrega | Tempo apos reprocessar | DLQ ao final |"
  echo "|---|---|---|---|---|---|---|---|"
  echo "| $N | $DLQ_ANTES | $total | $unicos | $dup | $taxa% | ${seg} s | $dlq |"
} > "$SAIDA/resumo-h2.md"

if awk "BEGIN{exit !($unicos*100 >= 99*$N)}" && [ "$dup" = 0 ]; then
  echo ">> OK: H2 validada, $taxa% das confirmacoes entregues e nenhum e-mail duplicado"
else
  echo ">> FALHA: entrega $taxa%, duplicados $dup"; exit 1
fi
