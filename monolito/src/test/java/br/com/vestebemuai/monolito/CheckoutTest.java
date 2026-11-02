package br.com.vestebemuai.monolito;

import br.com.vestebemuai.monolito.pedido.PedidoRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class CheckoutTest {

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

    @BeforeEach
    void setUp() {
        pedidos.deleteAll();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    @DisplayName("RF01: registra o pedido com itens (produto, tamanho e cor) e envia o e-mail")
    void registraPedidoComItens() throws Exception {
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.total").value(159.70))
                .andExpect(jsonPath("$.itens[0].tamanho").value("M"))
                .andExpect(jsonPath("$.itens[0].cor").value("Azul"));

        assertThat(pedidos.count()).isEqualTo(1);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Estado atual (P1): com o SMTP fora do ar o checkout falha e o pedido se perde")
    void checkoutFalhaComSmtpForaDoAr() throws Exception {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Pedido não concluído"));

        assertThat(pedidos.count()).isZero();
    }

    @Test
    @DisplayName("RF01: rejeita pedido sem itens ou com e-mail invalido")
    void rejeitaPedidoInvalido() throws Exception {
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteNome\":\"X\",\"clienteEmail\":\"invalido\",\"itens\":[]}"))
                .andExpect(status().isBadRequest());

        assertThat(pedidos.count()).isZero();
    }
}
