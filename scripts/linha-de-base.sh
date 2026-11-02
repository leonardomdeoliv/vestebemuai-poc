#!/usr/bin/env bash
# S1.5 - Linha de base (RNF01 / hipotese H1)
#
# Envia N pedidos ao monolitico em dois cenarios e mede quantos sao concluidos:
#   controle : Mailpit no ar      -> esperado: todos concluidos
#   falha    : Mailpit parado     -> estado atual: o checkout falha junto com o e-mail
#
# Uso: ./scripts/linha-de-base.sh [quantidade]   (padrao: 100)
# Saida: docs/sprints/sprint-1/evidencias/linha-de-base-<cenario>.csv e resumo.md
set -euo pipefail

N=${1:-100}
API=${API:-http://localhost:8080}
MAILPIT=${MAILPIT:-http://localhost:8025}
RAIZ=$(cd "$(dirname "$0")/.." && pwd)
SAIDA="$RAIZ/docs/sprints/sprint-1/evidencias"
mkdir -p "$SAIDA"

PRODUTOS=("Camiseta basica|M|Azul|59.90" "Calca jeans slim|42|Indigo|159.90" "Vestido midi floral|P|Verde|189.90"
          "Moletom canguru|G|Cinza|149.90" "Bone aba curva|U|Preto|39.90" "Tenis casual|40|Branco|229.90")

pedido_json() {
  local i=$1 p=${PRODUTOS[$(( $1 % ${#PRODUTOS[@]} ))]}
  IFS='|' read -r produto tamanho cor preco <<< "$p"
  printf '{"clienteNome":"Cliente Teste %03d","clienteEmail":"cliente%03d@exemplo.com","itens":[{"produto":"%s","tamanho":"%s","cor":"%s","quantidade":1,"precoUnitario":%s}]}' \
    "$i" "$i" "$produto" "$tamanho" "$cor" "$preco"
}

esperar_api() {
  for _ in $(seq 1 60); do curl -sf "$API/actuator/health" > /dev/null 2>&1 && return 0; sleep 1; done
  echo "API nao respondeu em $API" >&2; exit 1
}

rodar() {
  local cenario=$1 csv="$SAIDA/linha-de-base-$1.csv"
  echo "pedido,http_status,tempo_ms" > "$csv"
  local antes; antes=$(curl -s "$API/pedidos/total")
  local inicio; inicio=$(date +%s%3N)
  for i in $(seq 1 "$N"); do
    local r; r=$(curl -s -o /dev/null -w '%{http_code} %{time_total}' -X POST "$API/pedidos" \
      -H 'Content-Type: application/json' -d "$(pedido_json "$i")")
    local codigo=${r% *} tempo=${r#* }
    echo "$i,$codigo,$(awk "BEGIN{printf \"%.0f\", $tempo*1000}")" >> "$csv"
  done
  local duracao=$(( $(date +%s%3N) - inicio ))
  local depois; depois=$(curl -s "$API/pedidos/total")
  local ok; ok=$(awk -F, 'NR>1 && $2==201' "$csv" | wc -l)
  local erro; erro=$(awk -F, 'NR>1 && $2!=201' "$csv" | wc -l)
  local media; media=$(awk -F, 'NR>1{s+=$3;n++} END{printf "%.0f", s/n}' "$csv")
  echo "| $cenario | $N | $ok | $erro | $(( depois - antes )) | $media | $duracao |" >> "$SAIDA/resumo.tmp"
  echo "[$cenario] enviados=$N concluidos(201)=$ok falhas=$erro gravados_no_banco=$(( depois - antes )) tempo_medio=${media}ms"
}

cd "$RAIZ"
rm -f "$SAIDA/resumo.tmp"
esperar_api

echo ">> Cenario de controle: Mailpit no ar"
docker compose start mailpit > /dev/null
curl -s -X DELETE "$MAILPIT/api/v1/messages" > /dev/null
sleep 2
rodar controle
EMAILS=$(curl -s "$MAILPIT/api/v1/messages" | grep -o '"total":[0-9]*' | head -1 | cut -d: -f2)
echo "   e-mails na caixa do Mailpit: $EMAILS"

echo ">> Cenario de falha: Mailpit parado"
docker compose stop mailpit > /dev/null
rodar falha
docker compose start mailpit > /dev/null

{
  echo "# Linha de base - checkout no monolitico (estado atual)"
  echo
  echo "Executado em $(date '+%d/%m/%Y %H:%M') com $N pedidos por cenario."
  echo
  echo "| Cenario | Enviados | Concluidos (HTTP 201) | Falhas | Gravados no banco | Tempo medio (ms) | Duracao total (ms) |"
  echo "|---|---|---|---|---|---|---|"
  cat "$SAIDA/resumo.tmp"
  echo
  echo "E-mails recebidos pelo Mailpit no cenario de controle: $EMAILS"
} > "$SAIDA/resumo.md"
rm -f "$SAIDA/resumo.tmp"
cat "$SAIDA/resumo.md"
