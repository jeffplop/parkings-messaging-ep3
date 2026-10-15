package cl.duoc.parkings.messaging.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumidor de las Dead Letter Queues (rúbrica indicador 5 / instrucción
 * "manejar mensajes no entregados enviándolos a DLQ y registrándolos en logs").
 *
 * Escucha las tres DLQ de dominio y, por cada mensaje muerto, emite una ALERTA
 * en el log (nivel ERROR) con la causa original que RabbitMQ guarda en la
 * cabecera x-death. Lleva además un contador y, si se supera un umbral, eleva
 * la alerta — así cubre el punto "alertas si aplica" de la Prueba 2. En
 * producción, este mismo punto enviaría la alerta a correo/Slack/PagerDuty.
 *
 * Nota sobre retención: las DLQ se declaran con x-message-ttl y x-max-length
 * (ver RabbitTopologyConfig). Esa política es la red de seguridad para cuando
 * NO hay un consumidor drenando la DLQ (p. ej. este proceso caído): evita que
 * la cola de muertos crezca sin límite.
 */
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    /** Umbral de alerta: a partir de este número de mensajes muertos, se escala. */
    private static final int UMBRAL_ALERTA = 5;

    private final AtomicInteger muertosAcumulados = new AtomicInteger();

    @RabbitListener(queues = {
            "${parkings.rabbit.domains.notifications.dlq}",
            "${parkings.rabbit.domains.tickets.dlq}",
            "${parkings.rabbit.domains.documents.dlq}"
    }, containerFactory = "manualAckContainerFactory")
    public void onDeadLetter(Message message,
                             Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                             @Header(name = "x-death", required = false) Object xDeath) throws IOException {
        String cola = message.getMessageProperties().getConsumerQueue();
        String cuerpo = new String(message.getBody());
        int total = muertosAcumulados.incrementAndGet();

        log.error("⚠️ [ALERTA-DLQ] Mensaje muerto #{} en cola='{}'. x-death={} | payload={}",
                total, cola, xDeath, cuerpo);
        if (total >= UMBRAL_ALERTA) {
            log.error("🚨 [ALERTA-DLQ] Se superó el umbral de {} mensajes muertos: revisar el sistema.",
                    UMBRAL_ALERTA);
        }
        // Registrado/alertado: confirmamos para retirarlo de la DLQ.
        channel.basicAck(tag, false);
    }
}
