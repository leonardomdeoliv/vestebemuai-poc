package br.com.vestebemuai.notificacoes.evento;

import br.com.vestebemuai.notificacoes.email.EnvioEmailService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra do servico: cada evento gera no maximo um e-mail (RNF02).
 *
 * O registro do evento e o envio ficam na mesma transacao. Se o SMTP falhar, a
 * transacao volta, o registro nao fica gravado e a proxima tentativa envia de novo.
 * Se a mesma mensagem chegar outra vez depois do envio, o registro ja existe e ela
 * e ignorada.
 */
@Service
public class NotificacaoPedidoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPedidoService.class);

    private final EventoProcessadoRepository processados;
    private final EnvioEmailService email;
    private final MeterRegistry metricas;

    public NotificacaoPedidoService(EventoProcessadoRepository processados, EnvioEmailService email,
                                    MeterRegistry metricas) {
        this.processados = processados;
        this.email = email;
        this.metricas = metricas;
    }

    /** @return true se o e-mail foi enviado; false se o evento ja tinha sido processado. */
    @Transactional
    public boolean notificar(PedidoCriadoEvento evento) {
        if (processados.existsById(evento.eventoId())) {
            log.warn("Evento {} do pedido {} ja processado: mensagem duplicada ignorada, nenhum e-mail enviado",
                    evento.eventoId(), evento.pedidoId());
            metricas.counter("notificacoes.eventos.duplicados").increment();
            return false;
        }
        processados.saveAndFlush(new EventoProcessado(evento.eventoId(), evento.pedidoId()));
        email.enviarConfirmacao(evento);
        log.info("E-mail de confirmacao do pedido {} enviado para {} (evento {})",
                evento.pedidoId(), evento.clienteEmail(), evento.eventoId());
        metricas.counter("notificacoes.emails.enviados").increment();
        return true;
    }
}
