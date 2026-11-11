package br.com.vestebemuai.monolito;

import br.com.vestebemuai.monolito.notificacao.PedidoCriadoEvento;
import br.com.vestebemuai.monolito.pedido.PedidoRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modo padrao da Sprint 2: o checkout publica PedidoCriado e nao chama mais o SMTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class CheckoutFilaTest {

    private static final String PEDIDO = """
            {
              "clienteNome": "Maria Teste",
              "clienteEmail": "maria@exemplo.com",
              "itens": [
                {"produto": "Camiseta basica", "tamanho": "M", "cor": "Azul", "quantidade": 2, "precoUnitario": 59.90},
                {"produto": "Bone aba curva", "tamanho": "U", "cor": "Preto", "quantidade": 1, "precoUnitario": 39.90}
              ]
            }
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    PedidoRepository pedidos;

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    RabbitTemplate rabbit;

    @BeforeEach
    void setUp() {
        pedidos.deleteAll();
    }

    @Test
    @DisplayName("RF02: ao concluir o pedido publica PedidoCriado no exchange pedidos, sem chamar o SMTP")
    void publicaPedidoCriado() throws Exception {
        String corpo = mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andReturn().getResponse().getContentAsString();

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(rabbit, times(1)).convertAndSend(eq("pedidos"), eq("pedido.criado"), evento.capture(),
                any(MessagePostProcessor.class));

        PedidoCriadoEvento publicado = (PedidoCriadoEvento) evento.getValue();
        assertThat(publicado.eventoId()).isNotNull();
        assertThat(corpo).contains(publicado.pedidoId().toString());
        assertThat(publicado.clienteEmail()).isEqualTo("maria@exemplo.com");
        assertThat(publicado.itens()).hasSize(2);
        assertThat(publicado.total()).isEqualByComparingTo(new BigDecimal("159.70"));
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("S2.6 / H1: o pedido e concluido mesmo que o SMTP esteja fora do ar")
    void concluiSemDependerDoSmtp() throws Exception {
        doThrow(new IllegalStateException("SMTP fora do ar")).when(mailSender).send(any(MimeMessage.class));

        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated());

        assertThat(pedidos.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF02: com o RabbitMQ fora do ar o pedido continua gravado (falha so no log)")
    void pedidoGravadoMesmoSemBroker() throws Exception {
        doThrow(new AmqpConnectException(new java.net.ConnectException("Connection refused")))
                .when(rabbit).convertAndSend(eq("pedidos"), eq("pedido.criado"), any(Object.class),
                        any(MessagePostProcessor.class));

        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated());

        assertThat(pedidos.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF02: pedido invalido nao publica evento")
    void pedidoInvalidoNaoPublica() throws Exception {
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteNome\":\"X\",\"clienteEmail\":\"invalido\",\"itens\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(rabbit);
        assertThat(pedidos.count()).isZero();
    }
}
