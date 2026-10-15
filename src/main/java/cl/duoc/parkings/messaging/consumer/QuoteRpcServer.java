package cl.duoc.parkings.messaging.consumer;

import cl.duoc.parkings.messaging.event.QuoteRequest;
import cl.duoc.parkings.messaging.event.QuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Servidor de mensajería SÍNCRONA (RPC) — "Fundamental para la Prueba 2".
 *
 * Escucha la cola de peticiones y, al RETORNAR un {@link QuoteResponse}, Spring
 * AMQP lo publica automáticamente en la cola de respuesta del cliente
 * (Direct Reply-to). Usa ACK automático (autoAckContainerFactory): si el método
 * retorna sin excepción, el mensaje se confirma.
 */
@Component
public class QuoteRpcServer {

    private static final Logger log = LoggerFactory.getLogger(QuoteRpcServer.class);

    /** Tarifa por hora (CLP) según zona del estacionamiento. */
    private static final Map<String, BigDecimal> TARIFA_POR_ZONA = Map.of(
            "CENTRO", new BigDecimal("1800"),
            "RESIDENCIAL", new BigDecimal("900"),
            "AEROPUERTO", new BigDecimal("2500"));

    private static final BigDecimal TARIFA_DEFECTO = new BigDecimal("1200");

    @RabbitListener(queues = "${parkings.rabbit.rpc.queue}",
            containerFactory = "autoAckContainerFactory")
    public QuoteResponse cotizar(QuoteRequest request) {
        if (request.horas() <= 0) {
            throw new IllegalArgumentException("Las horas deben ser mayores a 0");
        }
        String zona = request.zona() == null ? "" : request.zona().toUpperCase();
        BigDecimal tarifaHora = TARIFA_POR_ZONA.getOrDefault(zona, TARIFA_DEFECTO);
        BigDecimal total = tarifaHora.multiply(BigDecimal.valueOf(request.horas()));

        log.info("[rpc-server] cotizando reservaId={} zona={} horas={} → total={}",
                request.reservaId(), zona, request.horas(), total);

        return new QuoteResponse(request.reservaId(), tarifaHora, total, "CLP");
    }
}
