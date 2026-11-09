package br.com.vestebemuai.monolito.pedido;

import br.com.vestebemuai.monolito.notificacao.EmailConfirmacaoService;
import br.com.vestebemuai.monolito.notificacao.PedidoCriadoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout do monolitico.
 *
 * Sprint 2 (Strangler Fig, S2.6): no modo "fila" (padrao) o checkout so grava o
 * pedido e registra o evento PedidoCriado, que e publicado no RabbitMQ depois do
 * commit. O e-mail passa a ser responsabilidade do servico de notificacoes, entao
 * uma falha no SMTP nao derruba mais o pedido.
 *
 * O modo "sincrono" mantem o comportamento da Sprint 1 (e-mail na mesma transacao)
 * so para comparar antes e depois na Sprint 3 e permitir voltar atras.
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final PedidoRepository pedidos;
    private final EmailConfirmacaoService emailConfirmacao;
    private final ApplicationEventPublisher eventos;
    private final ModoNotificacao modo;

    public CheckoutService(PedidoRepository pedidos, EmailConfirmacaoService emailConfirmacao,
                           ApplicationEventPublisher eventos,
                           @Value("${vestebemuai.notificacao.modo:fila}") ModoNotificacao modo) {
        this.pedidos = pedidos;
        this.emailConfirmacao = emailConfirmacao;
        this.eventos = eventos;
        this.modo = modo;
        log.info("Checkout iniciado com notificacao no modo {}", modo);
    }

    @Transactional
    public Pedido concluir(NovoPedidoRequest request) {
        Pedido pedido = new Pedido(request.clienteNome(), request.clienteEmail());
        request.itens().forEach(i -> pedido.adicionarItem(
                new ItemPedido(i.produto(), i.tamanho(), i.cor(), i.quantidade(), i.precoUnitario())));
        pedidos.save(pedido);

        if (modo == ModoNotificacao.SINCRONO) {
            // Modo antigo (Sprint 1): o checkout espera o SMTP responder.
            emailConfirmacao.enviar(pedido);
        } else {
            // Modo novo: publicado no RabbitMQ so depois do commit (PublicadorPedidoCriado).
            eventos.publishEvent(PedidoCriadoEvento.de(pedido));
        }

        log.info("Pedido {} concluido, total R$ {}", pedido.getId(), pedido.getTotal());
        return pedido;
    }
}
