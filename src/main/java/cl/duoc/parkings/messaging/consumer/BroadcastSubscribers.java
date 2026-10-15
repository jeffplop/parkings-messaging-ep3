package cl.duoc.parkings.messaging.consumer;

import cl.duoc.parkings.messaging.event.ReservaConfirmadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Suscriptores del patrón Publish/Subscribe — "Fundamental para la Prueba 2".
 *
 * El productor publica UN evento "reserva confirmada" en el exchange fanout y
 * RabbitMQ entrega una COPIA a cada cola suscriptora. Aquí dos suscriptores
 * independientes reaccionan al MISMO evento sin conocerse entre sí:
 *   - email:     notifica al conductor,
 *   - analytics: registra una métrica de negocio.
 *
 * Añadir un tercer suscriptor (p. ej. facturación) no requiere tocar al
 * productor: solo se declara otra cola enlazada al fanout.
 */
@Component
public class BroadcastSubscribers {

    private static final Logger log = LoggerFactory.getLogger(BroadcastSubscribers.class);

    @RabbitListener(queues = "${parkings.rabbit.broadcast.email-queue}",
            containerFactory = "autoAckContainerFactory")
    public void onEmail(ReservaConfirmadaEvent event) {
        log.info("[sub:email] Enviando correo de confirmación al conductor {} (reserva {})",
                event.cliente(), event.reservaId());
    }

    @RabbitListener(queues = "${parkings.rabbit.broadcast.analytics-queue}",
            containerFactory = "autoAckContainerFactory")
    public void onAnalytics(ReservaConfirmadaEvent event) {
        log.info("[sub:analytics] Registrando métrica: reserva {} confirmada (patente {})",
                event.reservaId(), event.patente());
    }
}
