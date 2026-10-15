package cl.duoc.parkings.messaging.event;

/**
 * Petición SÍNCRONA (RPC): cotizar el valor de una reserva de estacionamiento.
 * El cliente envía este mensaje y espera la {@link QuoteResponse} correspondiente.
 *
 * @param reservaId identificador de la reserva a cotizar
 * @param zona      zona tarifaria (p. ej. CENTRO, RESIDENCIAL, AEROPUERTO)
 * @param horas     cantidad de horas solicitadas
 */
public record QuoteRequest(String reservaId, String zona, int horas) {
}
