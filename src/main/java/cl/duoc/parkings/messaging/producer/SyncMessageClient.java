package cl.duoc.parkings.messaging.producer;

import cl.duoc.parkings.messaging.config.RabbitProperties;
import cl.duoc.parkings.messaging.event.QuoteRequest;
import cl.duoc.parkings.messaging.event.QuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Cliente de mensajería SÍNCRONA (RPC request/reply) — "Fundamental para la
 * Prueba 2".
 *
 * A diferencia del {@link MessagePublisher} (asíncrono, "dispara y olvida"),
 * aquí el emisor ENVÍA una petición y SE BLOQUEA esperando la respuesta, usando
 * {@code convertSendAndReceive}. Spring AMQP gestiona la cola de respuesta
 * temporal (Direct Reply-to) y correla la petición con su respuesta.
 */
@Service
public class SyncMessageClient {

    private static final Logger log = LoggerFactory.getLogger(SyncMessageClient.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties props;

    public SyncMessageClient(RabbitTemplate rabbitTemplate, RabbitProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    /** Cotiza una reserva de forma síncrona: devuelve el total ya calculado. */
    public QuoteResponse cotizar(QuoteRequest request) {
        RabbitProperties.Rpc rpc = props.getRpc();
        log.info("[rpc→] enviando cotización y esperando respuesta. reservaId={}", request.reservaId());

        Object reply = rabbitTemplate.convertSendAndReceive(
                rpc.getExchange(), rpc.getRoutingKey(), request);

        if (reply == null) {
            throw new IllegalStateException(
                    "El servidor RPC no respondió a tiempo (timeout) para la reserva " + request.reservaId());
        }
        QuoteResponse response = (QuoteResponse) reply;
        log.info("[rpc←] respuesta recibida. reservaId={} total={} {}",
                response.reservaId(), response.total(), response.moneda());
        return response;
    }
}
