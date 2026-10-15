package cl.duoc.parkings.messaging.event;

import java.math.BigDecimal;

/**
 * Respuesta SÍNCRONA (RPC) a una {@link QuoteRequest}: el total a pagar por la
 * reserva. Viaja de vuelta al cliente por la cola de respuesta (Direct Reply-to).
 *
 * @param reservaId  reserva cotizada
 * @param tarifaHora valor por hora según la zona (CLP)
 * @param total      total calculado = tarifaHora * horas (CLP)
 * @param moneda     moneda del monto (CLP)
 */
public record QuoteResponse(String reservaId, BigDecimal tarifaHora, BigDecimal total, String moneda) {
}
