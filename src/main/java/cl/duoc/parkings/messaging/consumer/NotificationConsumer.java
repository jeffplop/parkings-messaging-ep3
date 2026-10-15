package cl.duoc.parkings.messaging.consumer;

import cl.duoc.parkings.messaging.event.NotificationEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor del dominio NOTIFICACIONES de Parkings Together: avisa a conductores
 * y arrendadores (reserva confirmada, pago recibido, recordatorios).
 *
 * Confirmación de mensajes EXPLÍCITA (rúbrica indicador 5):
 *   - éxito                → basicAck
 *   - error recuperable    → basicNack con requeue (un reintento); si ya venía
 *                            redelivered, se manda a la DLQ para no reprocesar en bucle
 *   - error no recuperable → basicNack sin requeue → va a la DLQ
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @RabbitListener(queues = "${parkings.rabbit.domains.notifications.queue}",
            containerFactory = "manualAckContainerFactory")
    public void onNotification(@Payload NotificationEvent event,
                               Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                               @Header(name = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered)
            throws IOException {
        try {
            procesar(event);
            channel.basicAck(tag, false);
            log.info("[notifications] ACK id={} destinatario={}", event.id(), event.destinatario());
        } catch (TransientProcessingException e) {
            boolean yaReintentado = Boolean.TRUE.equals(redelivered);
            channel.basicNack(tag, false, !yaReintentado); // reintenta una vez; luego DLQ
            log.warn("[notifications] error recuperable ({}). {}", e.getMessage(),
                    yaReintentado ? "Segundo fallo → DLQ" : "Reintentando (requeue)");
        } catch (Exception e) {
            channel.basicNack(tag, false, false); // error no recuperable → DLQ
            log.error("[notifications] error NO recuperable → DLQ. id={} causa={}",
                    event.id(), e.toString());
        }
    }

    /** Lógica de negocio del envío (correo/SMS/push). Aquí iría el gateway real. */
    private void procesar(NotificationEvent event) {
        if (event.destinatario() == null || event.destinatario().isBlank()) {
            throw new IllegalArgumentException("Notificación sin destinatario"); // no recuperable
        }
        log.info("[notifications] Enviando {} a {}: {}", event.canal(), event.destinatario(), event.asunto());
    }
}
