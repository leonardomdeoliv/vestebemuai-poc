package br.com.vestebemuai.notificacoes.email;

import br.com.vestebemuai.notificacoes.evento.PedidoCriadoEvento;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Envia o e-mail de confirmacao do pedido pelo SMTP (Mailpit na PoC). */
@Service
public class EnvioEmailService {

    private final JavaMailSender mailSender;
    private final ModeloEmail modelo;
    private final String remetente;
    private final String assunto;

    public EnvioEmailService(JavaMailSender mailSender, ModeloEmail modelo,
                             @Value("${vestebemuai.email.remetente}") String remetente,
                             @Value("${vestebemuai.email.assunto}") String assunto) {
        this.mailSender = mailSender;
        this.modelo = modelo;
        this.remetente = remetente;
        this.assunto = assunto;
    }

    public void enviarConfirmacao(PedidoCriadoEvento evento) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(evento.clienteEmail());
            helper.setSubject(assunto);
            helper.setText(modelo.montar(evento), true);
            mailSender.send(mensagem);
        } catch (MessagingException e) {
            throw new MailSendException("Falha ao montar o e-mail do pedido " + evento.pedidoId(), e);
        }
    }
}
