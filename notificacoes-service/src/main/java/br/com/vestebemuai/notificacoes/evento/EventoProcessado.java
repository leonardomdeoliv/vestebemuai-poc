package br.com.vestebemuai.notificacoes.evento;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Registro de um evento que ja gerou e-mail (RNF02, idempotencia). */
@Entity
@Table(name = "evento_processado")
public class EventoProcessado {

    @Id
    private UUID eventoId;

    private UUID pedidoId;

    private OffsetDateTime processadoEm;

    protected EventoProcessado() {
    }

    public EventoProcessado(UUID eventoId, UUID pedidoId) {
        this.eventoId = eventoId;
        this.pedidoId = pedidoId;
        this.processadoEm = OffsetDateTime.now();
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public UUID getPedidoId() {
        return pedidoId;
    }

    public OffsetDateTime getProcessadoEm() {
        return processadoEm;
    }
}
