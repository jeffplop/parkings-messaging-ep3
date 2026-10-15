package cl.duoc.parkings.messaging.event;

/**
 * Evento de difusión (Publish/Subscribe): una reserva fue confirmada. Se publica
 * en el exchange fanout y lo reciben TODOS los suscriptores (correo, analítica,
 * etc.), cada uno con su propia reacción desacoplada.
 *
 * @param reservaId identificador de la reserva confirmada
 * @param cliente   nombre del conductor que reservó
 * @param patente   patente del vehículo
 * @param instante  marca de tiempo ISO-8601 de la confirmación
 */
public record ReservaConfirmadaEvent(String reservaId, String cliente, String patente, String instante) {
}
