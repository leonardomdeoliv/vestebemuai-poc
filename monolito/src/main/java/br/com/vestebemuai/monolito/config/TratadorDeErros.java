package br.com.vestebemuai.monolito.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    // Falha no SMTP derruba o checkout: o pedido nao e gravado (rollback).
    @ExceptionHandler(MailException.class)
    public ProblemDetail falhaNoEmail(MailException e) {
        log.error("Checkout falhou porque o e-mail nao foi enviado: {}", e.getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível concluir o pedido: falha ao enviar o e-mail de confirmação.");
        problema.setTitle("Pedido não concluído");
        return problema;
    }
}
