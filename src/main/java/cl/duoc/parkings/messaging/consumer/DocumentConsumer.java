package cl.duoc.parkings.messaging.consumer;

import cl.duoc.parkings.messaging.event.DocumentEvent;
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
 * Consumidor del dominio DOCUMENTOS de Parkings Together: genera documentos
 * asociados a una reserva (factura, comprobante PDF). Asíncrono y desacoplado.
 * Confirmación explícita (ACK/NACK/DLQ).
 */
@Component
public class DocumentConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentConsumer.class);

    @RabbitListener(queues = "${parkings.rabbit.domains.documents.queue}",
            containerFactory = "manualAckContainerFactory")
    public void onDocument(@Payload DocumentEvent event,
                           Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                           @Header(name = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered)
            throws IOException {
        try {
            procesar(event);
            channel.basicAck(tag, false);
            log.info("[documents] ACK reservaId={} tipo={} formato={}",
                    event.reservaId(), event.tipo(), event.formato());
        } catch (TransientProcessingException e) {
            boolean yaReintentado = Boolean.TRUE.equals(redelivered);
            channel.basicNack(tag, false, !yaReintentado);
            log.warn("[documents] error recuperable ({}). {}", e.getMessage(),
                    yaReintentado ? "Segundo fallo → DLQ" : "Reintentando (requeue)");
        } catch (Exception e) {
            channel.basicNack(tag, false, false);
            log.error("[documents] error NO recuperable → DLQ. reservaId={} causa={}",
                    event.reservaId(), e.toString());
        }
    }

    /** Generación del documento (factura/comprobante). */
    private void procesar(DocumentEvent event) {
        if (event.reservaId() == null || event.reservaId().isBlank()) {
            throw new IllegalArgumentException("Documento sin reservaId");
        }
        log.info("[documents] Generando {} ({}) de la reserva {}",
                event.tipo(), event.formato(), event.reservaId());
    }
}
