package br.com.vestebemuai.monolito.pedido;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final CheckoutService checkout;
    private final PedidoRepository pedidos;

    public PedidoController(CheckoutService checkout, PedidoRepository pedidos) {
        this.checkout = checkout;
        this.pedidos = pedidos;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody NovoPedidoRequest request) {
        Pedido pedido = checkout.concluir(request);
        return ResponseEntity.created(URI.create("/pedidos/" + pedido.getId())).body(PedidoResponse.de(pedido));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable UUID id) {
        return pedidos.findById(id)
                .map(p -> ResponseEntity.ok(PedidoResponse.de(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return pedidos.findAll().stream().map(PedidoResponse::de).toList();
    }

    @GetMapping("/total")
    public long total() {
        return pedidos.count();
    }
}
