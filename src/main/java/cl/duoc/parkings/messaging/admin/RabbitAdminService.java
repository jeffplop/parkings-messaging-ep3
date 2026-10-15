package cl.duoc.parkings.messaging.admin;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio que ENCAPSULA toda la lógica de administración de la topología de
 * RabbitMQ (rúbrica indicador 7). El controller llama a métodos de alto nivel y
 * no conoce los detalles de la librería AMQP; aquí se usa {@link AmqpAdmin}.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public void createQueue(String name, boolean durable) {
        amqpAdmin.declareQueue(new Queue(name, durable));
        log.info("[admin] Cola creada: {} (durable={})", name, durable);
    }

    public boolean deleteQueue(String name) {
        boolean deleted = amqpAdmin.deleteQueue(name);
        log.info("[admin] Cola eliminada: {} -> {}", name, deleted);
        return deleted;
    }

    public void createExchange(String name, String type, boolean durable) {
        Exchange exchange = switch (type) {
            case "direct"  -> ExchangeBuilder.directExchange(name).durable(durable).build();
            case "fanout"  -> ExchangeBuilder.fanoutExchange(name).durable(durable).build();
            case "headers" -> ExchangeBuilder.headersExchange(name).durable(durable).build();
            default         -> ExchangeBuilder.topicExchange(name).durable(durable).build();
        };
        amqpAdmin.declareExchange(exchange);
        log.info("[admin] Exchange creado: {} (type={}, durable={})", name, type, durable);
    }

    public void deleteExchange(String name) {
        amqpAdmin.deleteExchange(name);
        log.info("[admin] Exchange eliminado: {}", name);
    }

    public void createBinding(String queue, String exchange, String routingKey) {
        Binding binding = new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
        amqpAdmin.declareBinding(binding);
        log.info("[admin] Binding creado: {} -> {} (rk={})", exchange, queue, routingKey);
    }
}
