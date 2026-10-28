/*
 * C4 Model do ESTADO ATUAL da VesteBemUai (Sprint 1, S1.4).
 * Pode ser aberto em https://structurizr.com/dsl ou no Structurizr Lite.
 */
workspace "VesteBemUai - estado atual" "Monolitico com envio de e-mail sincrono no checkout" {

    model {
        cliente = person "Cliente" "Compra roupas e acessorios na loja online."
        gerente = person "Gerente de e-commerce" "Acompanha vendas e chamados (persona Thiago Braga)."

        loja = softwareSystem "Loja VesteBemUai" "Monolitico que concentra pedidos, estoque, pagamentos e notificacoes." {
            api = container "Monolitico" "Checkout, pedidos e envio do e-mail de confirmacao no mesmo processo." "Java 21, Spring Boot 3" {
                checkout = component "CheckoutService" "Grava o pedido e chama o envio de e-mail na mesma transacao." "Spring @Service"
                email = component "EmailConfirmacaoService" "Monta e envia o e-mail de confirmacao." "Spring @Service, JavaMailSender"
            }
            db = container "Banco de dados" "Pedidos e itens." "PostgreSQL 16" "Database"
            broker = container "Broker de mensagens" "Provisionado na Sprint 1, ainda sem uso. Recebera o evento PedidoCriado na Sprint 2." "RabbitMQ 3.13" "Futuro"
        }

        smtp = softwareSystem "Servidor de e-mail" "Entrega os e-mails. Na PoC e simulado pelo Mailpit." "Externo"

        cliente -> api "Faz pedidos" "HTTPS/JSON"
        gerente -> api "Consulta pedidos" "HTTPS/JSON"
        api -> db "Le e grava pedidos" "JDBC"
        api -> smtp "Envia a confirmacao durante o checkout (sincrono)" "SMTP"
        smtp -> cliente "Entrega o e-mail de confirmacao"
        checkout -> email "Chama e espera o envio"
        email -> smtp "Envia" "SMTP"
        checkout -> db "Grava o pedido" "JPA"
    }

    views {
        systemContext loja "Contexto" {
            include *
            autolayout lr
        }
        container loja "Conteineres" {
            include *
            autolayout lr
        }
        styles {
            element "Person" { shape Person; background #08427b; color #ffffff }
            element "Software System" { background #1168bd; color #ffffff }
            element "Container" { background #438dd5; color #ffffff }
            element "Database" { shape Cylinder }
            element "Externo" { background #999999; color #ffffff }
            element "Futuro" { background #ffffff; color #438dd5; border dashed }
        }
    }
}
