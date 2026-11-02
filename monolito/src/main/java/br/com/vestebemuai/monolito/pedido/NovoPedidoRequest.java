package br.com.vestebemuai.monolito.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record NovoPedidoRequest(
        @NotBlank String clienteNome,
        @NotBlank @Email String clienteEmail,
        @NotEmpty List<@Valid Item> itens) {

    public record Item(
            @NotBlank String produto,
            @NotBlank String tamanho,
            @NotBlank String cor,
            @Min(1) int quantidade,
            @NotNull @DecimalMin("0.01") BigDecimal precoUnitario) {
    }
}
