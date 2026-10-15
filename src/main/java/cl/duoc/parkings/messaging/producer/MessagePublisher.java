package cl.duoc.parkings.messaging.producer;

import cl.duoc.parkings.messaging.config.RabbitProperties;
import cl.duoc.parkings.messaging.event.DocumentEvent;
import cl.duoc.parkings.messaging.event.NotificationEvent;
import cl.duoc.parkings.messaging.event.ReservaConfirmadaEvent;
import cl.duoc.parkings.messaging.event.TicketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Productor de mensajes. La lógica de negocio llama a estos métodos de alto
 * nivel sin conocer exchanges ni routing keys (que se leen de RabbitProperties).
 * Así el desacople es real: publicar una tarea asíncrona no acopla al emisor
 * con la infraestructura de colas.
 */
@Service
public class MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties props;

    public MessagePublisher(RabbitTemplate rabbitTemplate, RabbitProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publishNotification(NotificationEvent event) {
        publish("notifications", event);
    }

    public void publishTicket(TicketEvent event) {
        publish("tickets", event);
    }

    public void publishDocument(DocumentEvent event) {
        publish("documents", event);
    }

    /**
     * Publish/Subscribe: difunde "reserva confirmada" al exchange FANOUT. Todos
     * los suscriptores (correo, analítica, ...) reciben una copia. El fanout
     * ignora la routing key, por eso se envía con clave vacía.
     */
    public void publishReservaConfirmada(ReservaConfirmadaEvent event) {
        rabbitTemplate.convertAndSend(props.getFanoutExchange(), "", event, this::persistente);
        log.info("[broadcast] fanout={} reservaId={} → todos los suscriptores",
                props.getFanoutExchange(), event.reservaId());
    }

    /** Publica un payload al exchange de trabajo con la routing key del dominio (asíncrono). */
    private void publish(String domain, Object payload) {
        RabbitProperties.Domain d = props.domain(domain);
        rabbitTemplate.convertAndSend(props.getExchange(), d.getRoutingKey(), payload, this::persistente);
        log.info("[publish] dominio={} exchange={} routingKey={} payload={}",
                domain, props.getExchange(), d.getRoutingKey(), payload);
    }

    /**
     * Durabilidad del mensaje: lo marca PERSISTENT para que RabbitMQ lo guarde en
     * disco. Combinado con colas durables, el mensaje sobrevive a un reinicio del
     * broker. (RabbitTemplate ya usa PERSISTENT por defecto; se deja explícito.)
     */
    private org.springframework.amqp.core.Message persistente(org.springframework.amqp.core.Message m) {
        m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return m;
    }
}
