# S3.2 - Teste de falha (H1): 100 pedidos com o Mailpit parado

Executado em 19/11/2026 22:37 em ip-172-31-28-44.ec2.internal.

| Modo do checkout | Enviados | Concluidos (HTTP 201) | Falhas | Gravados no banco | Tempo medio (ms) | Duracao total (ms) |
|---|---|---|---|---|---|---|
| sincrono | 100 | 0 | 100 | 0 | 21 | 3320 |
| fila | 100 | 100 | 0 | 100 | 30 | 4616 |

Ao final: fila principal com 0 prontas e 100 em processamento (reenvio); DLQ com 0.
