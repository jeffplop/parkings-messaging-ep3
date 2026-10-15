package cl.duoc.parkings.messaging.consumer;

import cl.duoc.parkings.messaging.event.TicketEvent;
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
 * Consumidor del dominio TICKETS de Parkings Together: genera el comprobante de
 * pago de una reserva (tras el cobro con Webpay). Procesamiento asíncrono y
 * desacoplado del flujo de pago. Confirmación explícita (ACK/NACK/DLQ).
 */
@Component
public class TicketConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketConsumer.class);

    @RabbitListener(queues = "${parkings.rabbit.domains.tickets.queue}",
            containerFactory = "manualAckContainerFactory")
    public void onTicket(@Payload TicketEvent event,
                         Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                         @Header(name = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered)
            throws IOException {
        try {
            procesar(event);
            channel.basicAck(tag, false);
            log.info("[tickets] ACK reservaId={} monto={}", event.reservaId(), event.monto());
        } catch (TransientProcessingException e) {
            boolean yaReintentado = Boolean.TRUE.equals(redelivered);
            channel.basicNack(tag, false, !yaReintentado);
            log.warn("[tickets] error recuperable ({}). {}", e.getMessage(),
                    yaReintentado ? "Segundo fallo → DLQ" : "Reintentando (requeue)");
        } catch (Exception e) {
            channel.basicNack(tag, false, false);
            log.error("[tickets] error NO recuperable → DLQ. reservaId={} causa={}",
                    event.reservaId(), e.toString());
        }
    }

    /** Generación del comprobante de pago de la reserva. */
    private void procesar(TicketEvent event) {
        if (event.reservaId() == null || event.reservaId().isBlank()) {
            throw new IllegalArgumentException("Ticket sin reservaId");
        }
        log.info("[tickets] Generando comprobante de pago de la reserva {} por {}",
                event.reservaId(), event.monto());
    }
}
