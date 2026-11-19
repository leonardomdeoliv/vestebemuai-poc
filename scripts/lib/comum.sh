# Funcoes comuns dos scripts da Sprint 3 (carregado com "source").
API=${API:-http://localhost:8080}
NOTIF=${NOTIF:-http://localhost:8081}
MAILPIT=${MAILPIT:-http://localhost:8025}
RABBIT=${RABBIT:-http://localhost:15672}
RABBIT_AUTH=${RABBIT_AUTH:-vestebemuai:vestebemuai}
FILA=notificacoes.pedido-criado
DLQ=notificacoes.dlq

PRODUTOS=("Camiseta basica|M|Azul|59.90" "Calca jeans slim|42|Indigo|159.90" "Vestido midi floral|P|Verde|189.90"
          "Moletom canguru|G|Cinza|149.90" "Bone aba curva|U|Preto|39.90" "Tenis casual|40|Branco|229.90")

agora_ms() { date +%s%3N; }

# pedido_json <numero> <prefixo do e-mail>
pedido_json() {
  local i=$1 prefixo=$2 p=${PRODUTOS[$(( $1 % ${#PRODUTOS[@]} ))]}
  IFS='|' read -r produto tamanho cor preco <<< "$p"
  printf '{"clienteNome":"Cliente Teste %03d","clienteEmail":"%s-%03d@exemplo.com","itens":[{"produto":"%s","tamanho":"%s","cor":"%s","quantidade":1,"precoUnitario":%s}]}' \
    "$i" "$prefixo" "$i" "$produto" "$tamanho" "$cor" "$preco"
}

esperar_url() { # esperar_url <url> [segundos]
  for _ in $(seq 1 "${2:-90}"); do curl -sf "$1" > /dev/null 2>&1 && return 0; sleep 1; done
  echo "Sem resposta de $1" >&2; return 1
}

# fila_info <fila> -> "prontas em_processamento"
fila_info() {
  curl -s -u "$RABBIT_AUTH" "$RABBIT/api/queues/%2F/$1" |
    python3 -c 'import json,sys; q=json.load(sys.stdin); print(q.get("messages_ready",0), q.get("messages_unacknowledged",0))'
}

esvaziar_fila() { curl -s -o /dev/null -u "$RABBIT_AUTH" -X DELETE "$RABBIT/api/queues/%2F/$1/contents"; }

# metrica <url-base> <nome> [tag] -> valor
metrica() {
  local url="$1/actuator/metrics/$2"; [ -n "${3:-}" ] && url="$url?tag=$3"
  curl -s "$url" | python3 -c 'import json,sys
try: print(int(json.load(sys.stdin)["measurements"][0]["value"]))
except Exception: print(0)'
}

# emails_por_prefixo <prefixo> -> "total unicos duplicados"
emails_por_prefixo() {
  curl -s "$MAILPIT/api/v1/search?query=to:$1&limit=1000" | python3 -c '
import json,sys,collections
pref=sys.argv[1]
msgs=json.load(sys.stdin).get("messages",[])
c=collections.Counter(t["Address"] for m in msgs for t in m["To"] if t["Address"].startswith(pref+"-"))
print(sum(c.values()), len(c), sum(v-1 for v in c.values() if v>1))' "$1"
}

trocar_modo() { # trocar_modo fila|sincrono  (recria so o monolito, sem religar o Mailpit)
  NOTIFICACAO_MODO=$1 docker compose up -d --no-deps monolito > /dev/null 2>&1
  sleep 3; esperar_url "$API/actuator/health" 120
}

# dlq_atual -> mensagens na DLQ lidas pelo servico (sem o atraso de 5 s das estatisticas do painel)
dlq_atual() { curl -s "$NOTIF/dlq" | python3 -c 'import json,sys; print(json.load(sys.stdin)["mensagens"])'; }
