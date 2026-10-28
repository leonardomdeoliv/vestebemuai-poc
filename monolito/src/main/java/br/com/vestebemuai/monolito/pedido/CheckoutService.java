package br.com.vestebemuai.monolito.pedido;

import br.com.vestebemuai.monolito.notificacao.EmailConfirmacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout do monolitico, no formato "estado atual" (antes da extracao).
 *
 * O pedido e gravado e o e-mail de confirmacao e enviado na MESMA transacao e
 * na MESMA thread da requisicao. Se o servidor de e-mail estiver fora do ar, o
 * envio lanca excecao, a transacao sofre rollback e o cliente perde o pedido.
 * Esse e o acoplamento que a Sprint 2 vai remover com o padrao Strangler Fig.
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final PedidoRepository pedidos;
    private final EmailConfirmacaoService emailConfirmacao;

    public CheckoutService(PedidoRepository pedidos, EmailConfirmacaoService emailConfirmacao) {
        this.pedidos = pedidos;
        this.emailConfirmacao = emailConfirmacao;
    }

    @Transactional
    public Pedido concluir(NovoPedidoRequest request) {
        Pedido pedido = new Pedido(request.clienteNome(), request.clienteEmail());
        request.itens().forEach(i -> pedido.adicionarItem(
                new ItemPedido(i.produto(), i.tamanho(), i.cor(), i.quantidade(), i.precoUnitario())));
        pedidos.save(pedido);

        // Envio sincrono: o checkout espera o SMTP responder.
        emailConfirmacao.enviar(pedido);

        log.info("Pedido {} concluido, total R$ {}", pedido.getId(), pedido.getTotal());
        return pedido;
    }
}
