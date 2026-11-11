/*
 * C4 Model da ARQUITETURA ALVO da VesteBemUai (Sprint 2, S2.1).
 * Notificacoes extraidas do monolitico com Strangler Fig (ADR-001 a ADR-003).
 * Pode ser aberto em https://structurizr.com/dsl ou no Structurizr Lite.
 */
workspace "VesteBemUai - arquitetura alvo" "Checkout publica PedidoCriado; o servico de notificacoes envia o e-mail" {

    model {
        cliente = person "Cliente" "Compra roupas e acessorios na loja online."
        equipe = person "Equipe de TI" "Acompanha a DLQ e reprocessa mensagens (persona Anderson Moura)."

        loja = softwareSystem "Loja VesteBemUai" "Loja online com o modulo de notificacoes extraido." {
            api = container "Monolitico" "Checkout e pedidos. Publica PedidoCriado depois do commit; envio sincrono desligado (modo=fila)." "Java 21, Spring Boot 3"
            db = container "Banco de dados" "schema public (pedido, item_pedido) e schema notificacoes (evento_processado)." "PostgreSQL 16" "Database"
            broker = container "Broker de mensagens" "Exchange pedidos, fila notificacoes.pedido-criado, DLX notificacoes.dlx e DLQ notificacoes.dlq." "RabbitMQ 3.13" "Fila"
            notificacoes = container "Servico de notificacoes" "Consome PedidoCriado, envia o e-mail com reenvio e DLQ, idempotente pelo eventoId." "Java 21, Spring Boot 3" "Novo" {
                listener = component "PedidoCriadoListener" "Consome o evento; retry 2 s, 4 s, 8 s; loga cada tentativa." "@RabbitListener"
                servico = component "NotificacaoPedidoService" "Idempotencia pelo eventoId e metricas." "@Service, @Transactional"
                repositorio = component "EventoProcessadoRepository" "Tabela evento_processado." "Spring Data JPA"
                envio = component "EnvioEmailService" "Monta e envia o e-mail." "@Service, JavaMailSender"
                modelo = component "ModeloEmail" "templates/email-confirmacao.html (RF06)." "@Component"
                dlq = component "DlqController" "GET /dlq e POST /dlq/reprocessar." "@RestController"
                config = component "RabbitConfig" "Declara exchange, fila, DLX e DLQ." "@Configuration"
            }
        }

        smtp = softwareSystem "Servidor de e-mail" "Entrega os e-mails. Na PoC e simulado pelo Mailpit." "Externo"

        cliente -> api "Faz pedidos" "HTTPS/JSON"
        api -> db "Le e grava pedidos" "JDBC"
        api -> broker "Publica PedidoCriado (assincrono)" "AMQP"
        broker -> listener "Entrega o evento" "AMQP"
        listener -> servico "notificar(evento)"
        servico -> repositorio "Verifica e grava o eventoId"
        repositorio -> db "Le e grava" "JDBC"
        servico -> envio "Envia a confirmacao"
        envio -> modelo "Usa o modelo"
        envio -> smtp "Envia" "SMTP"
        equipe -> dlq "Consulta e reprocessa a DLQ" "HTTP"
        dlq -> broker "Move mensagens da DLQ para a fila principal" "AMQP"
        config -> broker "Declara a topologia" "AMQP"
        smtp -> cliente "Entrega o e-mail de confirmacao"
    }

    views {
        container loja "ConteineresAlvo" {
            include *
            autolayout lr
        }
        component notificacoes "ComponentesNotificacoes" {
            include *
            autolayout lr
        }
        styles {
            element "Person" { shape Person; background #08427b; color #ffffff }
            element "Software System" { background #1168bd; color #ffffff }
            element "Container" { background #438dd5; color #ffffff }
            element "Component" { background #85bbf0; color #000000 }
            element "Database" { shape Cylinder }
            element "Fila" { shape Pipe }
            element "Novo" { background #1e7b34; color #ffffff }
            element "Externo" { background #999999; color #ffffff }
        }
    }
}
