#!/usr/bin/env bash
# S3.4 - Publicar so o servico de notificacoes e medir o tempo de deploy (RF06 / hipotese H3)
#
# 1. Acrescenta um aviso no modelo do e-mail (se ainda nao estiver la).
# 2. Enquanto envia 1 pedido a cada 0,5 s ao monolitico, reconstroi e republica so o
#    servico (docker compose up -d --build --no-deps notificacoes) e mede o tempo.
# 3. Confere: todos os pedidos responderam 201, o monolitico nao reiniciou e os
#    e-mails feitos durante a troca sairam com o modelo novo.
#
# Uso: ./scripts/deploy-notificacoes.sh ["texto do aviso"] [pasta de saida]
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/lib/comum.sh

AVISO=${1:-Trocas grátis em até 30 dias pelo site.}
SAIDA=${2:-docs/sprints/sprint-3/evidencias}
MODELO=notificacoes-service/src/main/resources/templates/email-confirmacao.html
PREFIXO=deploy$(date +%H%M%S)
mkdir -p "$SAIDA"

esperar_url "$API/actuator/health"; esperar_url "$NOTIF/actuator/health"
docker compose start mailpit > /dev/null 2>&1; esperar_url "$MAILPIT/livez" 30
INICIO_MONOLITO=$(docker inspect -f '{{.State.StartedAt}}' "$(docker compose ps -q monolito)")

echo ">> 1. Mudanca no modelo do e-mail (so no servico de notificacoes)"
if ! grep -qF "$AVISO" "$MODELO"; then
  AVISO_SED=$(printf '%s' "$AVISO" | sed 's/[&|\\]/\\&/g')
  sed -i "s|<!-- avisos: .*-->|&\n<p class=\"aviso\">$AVISO_SED</p>|" "$MODELO"
fi
grep -n "aviso\"" "$MODELO" | sed 's/^/   /'

echo ">> 2. Pedidos chegando ao monolitico durante o deploy (1 a cada 0,5 s)"
: > "$SAIDA/deploy-build.log"
CSV="$SAIDA/deploy-pedidos.csv"; echo "pedido,http_status" > "$CSV"
( i=0; while [ ! -f /tmp/fim-deploy ]; do i=$((i+1))
    c=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/pedidos" -H 'Content-Type: application/json' -d "$(pedido_json "$i" "$PREFIXO")")
    echo "$i,$c" >> "$CSV"; sleep 0.5; done ) &
CARGA=$!; rm -f /tmp/fim-deploy; sleep 2

echo ">> 3. docker compose up -d --build --no-deps notificacoes"
t0=$(agora_ms)
# PRE_BUILD: comando opcional antes do build (ex.: "mvn -q package" quando o Maven roda fora do Docker)
[ -n "${PRE_BUILD:-}" ] && bash -c "$PRE_BUILD" >> "$SAIDA/deploy-build.log" 2>&1
docker compose build notificacoes >> "$SAIDA/deploy-build.log" 2>&1
t1=$(agora_ms)
docker compose up -d --no-deps notificacoes > /dev/null 2>&1
for _ in $(seq 1 120); do
  [ "$(curl -s "$NOTIF/actuator/health" | python3 -c 'import json,sys
try: print(json.load(sys.stdin)["status"])
except Exception: print("")')" = UP ] && break; sleep 0.5
done
t2=$(agora_ms)
sleep 3; touch /tmp/fim-deploy; wait $CARGA; rm -f /tmp/fim-deploy

BUILD=$(awk "BEGIN{printf \"%.1f\", ($t1-$t0)/1000}")
TROCA=$(awk "BEGIN{printf \"%.1f\", ($t2-$t1)/1000}")
TOTAL=$(awk "BEGIN{printf \"%.1f\", ($t2-$t0)/1000}")
echo "   build da imagem: ${BUILD} s | troca do conteiner ate o health UP: ${TROCA} s | total: ${TOTAL} s"

ENVIADOS=$(awk 'NR>1' "$CSV" | wc -l); OK=$(awk -F, 'NR>1 && $2==201' "$CSV" | wc -l)
INICIO_DEPOIS=$(docker inspect -f '{{.State.StartedAt}}' "$(docker compose ps -q monolito)")
echo ">> 4. Monolitico durante o deploy: $OK de $ENVIADOS pedidos com HTTP 201"
[ "$INICIO_MONOLITO" = "$INICIO_DEPOIS" ] && echo "   monolitico nao reiniciou (no ar desde $INICIO_MONOLITO)" || echo "   ATENCAO: monolitico reiniciou"

for _ in $(seq 1 60); do read -r total unicos dup < <(emails_por_prefixo "$PREFIXO"); [ "$unicos" -ge "$ENVIADOS" ] && break; sleep 1; done
ULTIMO=$(curl -s "$MAILPIT/api/v1/search?query=to:$PREFIXO&limit=1" | python3 -c 'import json,sys; print(json.load(sys.stdin)["messages"][0]["ID"])')
COM_AVISO=$(curl -s "$MAILPIT/api/v1/message/$ULTIMO" | python3 -c 'import json,sys; print(1 if sys.argv[1] in json.load(sys.stdin)["HTML"] else 0)' "$AVISO")
echo ">> 5. E-mails dos pedidos feitos durante o deploy: $unicos de $ENVIADOS entregues, $dup duplicados; ultimo com o aviso novo: $([ "$COM_AVISO" -ge 1 ] && echo sim || echo nao)"

{
  echo "# S3.4 - Deploy so do servico de notificacoes (H3)"
  echo
  echo "Executado em $(date '+%d/%m/%Y %H:%M') em $(hostname)."
  echo
  echo "| Build da imagem | Troca do conteiner ate UP | Tempo total | Pedidos durante o deploy | HTTP 201 | Monolito reiniciou? | E-mails entregues | Modelo novo no e-mail |"
  echo "|---|---|---|---|---|---|---|---|"
  echo "| ${BUILD} s | ${TROCA} s | ${TOTAL} s | $ENVIADOS | $OK | $([ "$INICIO_MONOLITO" = "$INICIO_DEPOIS" ] && echo nao || echo sim) | $unicos | $([ "$COM_AVISO" -ge 1 ] && echo sim || echo nao) |"
} > "$SAIDA/resumo-h3.md"

if [ "$OK" = "$ENVIADOS" ] && [ "$INICIO_MONOLITO" = "$INICIO_DEPOIS" ] && [ "$COM_AVISO" -ge 1 ]; then
  echo ">> OK: H3 validada, servico publicado sozinho em ${TOTAL} s sem parar o checkout"
else echo ">> FALHA"; exit 1; fi
