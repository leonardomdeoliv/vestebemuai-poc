#!/usr/bin/env bash
# S3.2 - Teste de falha com 100 pedidos e o Mailpit parado (RNF01 / hipotese H1)
#
# Mesmo ambiente, mesmo SMTP fora do ar, dois modos do checkout:
#   sincrono : arquitetura antiga (Sprint 1), o e-mail e enviado dentro do checkout
#   fila     : arquitetura alvo (Sprint 2), o checkout so publica PedidoCriado no RabbitMQ
# Esperado: sincrono 0/100 (repete a linha de base) e fila 100/100.
# O Mailpit fica PARADO no final: a S3.3 (entrega-h2.sh) continua daqui.
#
# Uso: ./scripts/teste-falha-h1.sh [quantidade] [pasta de saida]
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/lib/comum.sh

N=${1:-100}
SAIDA=${2:-docs/sprints/sprint-3/evidencias}
mkdir -p "$SAIDA"
PREFIXO=h1

rodar() { # rodar <modo>
  local modo=$1 csv="$SAIDA/teste-falha-$1.csv"
  echo "pedido,http_status,tempo_ms" > "$csv"
  local antes; antes=$(curl -s "$API/pedidos/total")
  local inicio; inicio=$(agora_ms)
  for i in $(seq 1 "$N"); do
    local r; r=$(curl -s -o /dev/null -w '%{http_code} %{time_total}' -X POST "$API/pedidos" \
      -H 'Content-Type: application/json' -d "$(pedido_json "$i" "$PREFIXO-$modo")")
    echo "$i,${r% *},$(awk "BEGIN{printf \"%.0f\", ${r#* }*1000}")" >> "$csv"
  done
  local duracao=$(( $(agora_ms) - inicio ))
  local depois; depois=$(curl -s "$API/pedidos/total")
  local ok erro media
  ok=$(awk -F, 'NR>1 && $2==201' "$csv" | wc -l)
  erro=$(awk -F, 'NR>1 && $2!=201' "$csv" | wc -l)
  media=$(awk -F, 'NR>1{s+=$3;n++} END{printf "%.0f", s/n}' "$csv")
  local codigos; codigos=$(awk -F, 'NR>1{print $2}' "$csv" | sort | uniq -c | awk '{printf "%s HTTP %s; ", $1, $2}')
  printf '| %s | %d | %d | %d | %d | %s | %s |\n' "$modo" "$N" "$ok" "$erro" $(( depois - antes )) "$media" "$duracao" >> "$SAIDA/resumo-h1.tmp"
  echo "   [$modo] enviados=$N concluidos=$ok falhas=$erro gravados=$(( depois - antes )) tempo_medio=${media}ms ($codigos)"
}

esperar_url "$API/actuator/health"
rm -f "$SAIDA/resumo-h1.tmp"
echo ">> Preparacao: filas vazias e Mailpit limpo"
docker compose start mailpit > /dev/null 2>&1; esperar_url "$MAILPIT/livez" 30
curl -s -o /dev/null -X DELETE "$MAILPIT/api/v1/messages"
esvaziar_fila "$FILA"; esvaziar_fila "$DLQ"

echo ">> Mailpit PARADO (servidor de e-mail fora do ar)"
docker compose stop mailpit > /dev/null 2>&1

echo ">> 1. Checkout no modo sincrono (antes da extracao)"
trocar_modo sincrono
rodar sincrono

echo ">> 2. Checkout no modo fila (depois da extracao)"
trocar_modo fila
rodar fila

sleep 6 # as estatisticas do painel do RabbitMQ atualizam a cada 5 s
read -r prontas proc < <(fila_info "$FILA")
dlq=$(dlq_atual)
{
  echo "# S3.2 - Teste de falha (H1): $N pedidos com o Mailpit parado"
  echo
  echo "Executado em $(date '+%d/%m/%Y %H:%M') em $(hostname)."
  echo
  echo "| Modo do checkout | Enviados | Concluidos (HTTP 201) | Falhas | Gravados no banco | Tempo medio (ms) | Duracao total (ms) |"
  echo "|---|---|---|---|---|---|---|"
  cat "$SAIDA/resumo-h1.tmp"
  echo
  echo "Ao final: fila principal com $prontas prontas e $proc em processamento (reenvio); DLQ com $dlq."
} > "$SAIDA/resumo-h1.md"
rm -f "$SAIDA/resumo-h1.tmp"
echo ">> Eventos esperando o e-mail voltar: fila=$prontas em_reenvio=$proc dlq=$dlq"
OK_FILA=$(awk -F, 'NR>1 && $2==201' "$SAIDA/teste-falha-fila.csv" | wc -l)
if [ "$OK_FILA" = "$N" ]; then echo ">> OK: H1 validada, $OK_FILA de $N pedidos concluidos com o e-mail fora do ar"
else echo ">> FALHA: so $OK_FILA de $N pedidos concluidos no modo fila"; exit 1; fi
