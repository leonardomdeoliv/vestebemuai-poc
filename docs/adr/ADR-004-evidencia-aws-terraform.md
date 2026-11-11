# ADR-004: Evidência em nuvem com uma EC2 temporária criada por Terraform

- **Status:** Aceita
- **Data:** 09/11/2026
- **Decisores:** Leonardo (arquiteto e desenvolvedor do projeto)
- **Relacionada a:** RNF06; restrição de custo de US$ 5; Sprint 3

## Contexto

A Sprint 3 precisa repetir o teste de falha (100 pedidos com o e-mail parado) fora da máquina do autor,
para mostrar que a solução não depende do ambiente local (H3). O limite é de **US$ 5** no total, com o alerta
`vestebemuai-poc-budget` já criado no AWS Budgets na Sprint 1. Não há ambiente de produção nem clientes reais.

## Opções consideradas

| Opção | Custo estimado | Avaliação |
|---|---|---|
| **Uma EC2 t3.medium com o mesmo Docker Compose, criada e destruída com Terraform** | cerca de US$ 0,04/h; algumas horas de uso ficam bem abaixo de US$ 1 | Mesmo ambiente da máquina local; infraestrutura como código; fácil de destruir |
| ECS Fargate + Amazon MQ + RDS | Amazon MQ e RDS cobram por hora mesmo parados; facilmente acima de US$ 5 | Fora do escopo (serviços gerenciados) e do orçamento |
| EKS (Kubernetes) | Só o plano de controle custa cerca de US$ 0,10/h | Complexidade e custo desnecessários para a PoC |
| Criar a EC2 pelo console | Igual à primeira | Não é reproduzível e é fácil esquecer recursos ligados |

## Decisão

Na Sprint 3, um módulo Terraform cria: uma EC2 **t3.medium** (Amazon Linux 2023), um *security group* que
libera só as portas da demonstração para o IP do autor, e um *user data* que instala o Docker, clona o
repositório e roda `docker compose up -d`. O roteiro é: `terraform apply` → teste dos 100 pedidos → prints →
`terraform destroy` no mesmo dia.

## Consequências

- **Positivas:** o mesmo `docker-compose.yml` roda local e na nuvem (RNF04 e H3); o `destroy` garante que
  nada fica cobrando; o código Terraform vira evidência e fica no repositório.
- **Negativas:** sem alta disponibilidade (uma instância só), o que é aceitável para uma evidência;
  as credenciais da AWS ficam só na máquina do autor e não vão para o repositório.
- **Controle de custo:** alerta de 80% (real) e 100% (previsto) do orçamento; conferir o Cost Explorer
  depois do `destroy`.
