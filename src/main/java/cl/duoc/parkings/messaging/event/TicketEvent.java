package cl.duoc.parkings.messaging.event;

import java.math.BigDecimal;

/** Solicitud asíncrona de generación de un ticket/boleta para una orden. */
public record TicketEvent(
        String reservaId,
        String cliente,
        BigDecimal monto
) {}
