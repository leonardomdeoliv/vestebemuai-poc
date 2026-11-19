# S3.2 - Teste de falha (H1): 100 pedidos com o Mailpit parado

Executado em 19/11/2026 22:38 em vm.

| Modo do checkout | Enviados | Concluidos (HTTP 201) | Falhas | Gravados no banco | Tempo medio (ms) | Duracao total (ms) |
|---|---|---|---|---|---|---|
| sincrono | 100 | 0 | 100 | 0 | 15 | 2415 |
| fila | 100 | 100 | 0 | 100 | 16 | 2578 |

Ao final: fila principal com 0 prontas e 0 em processamento (reenvio); DLQ com 0.
