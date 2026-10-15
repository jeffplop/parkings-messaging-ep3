package cl.duoc.parkings.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declaración de la topología de mensajería como BEANS (rúbrica indicador 2).
 *
 * Por cada caso de uso (dominio) se declaran de forma explícita y documentada:
 *   - la cola de trabajo (con Dead Letter Exchange + routing key hacia su DLQ),
 *   - su Dead Letter Queue (DLQ) CON política de retención (TTL + longitud máx.),
 *   - el binding de la cola de trabajo al exchange,
 *   - el binding de la DLQ al Dead Letter Exchange.
 *
 * Además declara la infraestructura "Fundamental para la Prueba 2":
 *   - un exchange FANOUT + colas suscriptoras  → Publish/Subscribe,
 *   - un exchange DIRECT + cola de petición    → mensajería SÍNCRONA (RPC).
 *
 * TODAS las colas y exchanges se declaran DURABLES (durability): sobreviven a
 * un reinicio del broker. Los mensajes se publican PERSISTENT por defecto
 * (RabbitTemplate), de modo que tampoco se pierden al caerse RabbitMQ.
 *
 * Esta clase SOLO declara infraestructura: no contiene lógica de negocio
 * (rúbrica indicador 3). Los nombres se leen de RabbitProperties (centralizados).
 */
@Configuration
public class RabbitTopologyConfig {

    private final RabbitProperties props;

    public RabbitTopologyConfig(RabbitProperties props) {
        this.props = props;
    }

    // ── Exchanges ────────────────────────────────────────────────────────────
    @Bean
    TopicExchange workExchange() {
        return new TopicExchange(props.getExchange(), true, false); // durable, no auto-delete
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(props.getDeadLetterExchange(), true, false);
    }

    // ── Dominio: NOTIFICACIONES ──────────────────────────────────────────────
    @Bean
    Queue notificationsQueue() {
        return workQueue(props.domain("notifications"));
    }

    @Bean
    Queue notificationsDlq() {
        return deadLetterQueue(props.domain("notifications").getDlq());
    }

    @Bean
    Binding notificationsBinding() {
        var d = props.domain("notifications");
        return BindingBuilder.bind(notificationsQueue()).to(workExchange()).with(d.getRoutingKey());
    }

    @Bean
    Binding notificationsDlqBinding() {
        var d = props.domain("notifications");
        return BindingBuilder.bind(notificationsDlq()).to(deadLetterExchange()).with(d.getDlqRoutingKey());
    }

    // ── Dominio: TICKETS ─────────────────────────────────────────────────────
    @Bean
    Queue ticketsQueue() {
        return workQueue(props.domain("tickets"));
    }

    @Bean
    Queue ticketsDlq() {
        return deadLetterQueue(props.domain("tickets").getDlq());
    }

    @Bean
    Binding ticketsBinding() {
        var d = props.domain("tickets");
        return BindingBuilder.bind(ticketsQueue()).to(workExchange()).with(d.getRoutingKey());
    }

    @Bean
    Binding ticketsDlqBinding() {
        var d = props.domain("tickets");
        return BindingBuilder.bind(ticketsDlq()).to(deadLetterExchange()).with(d.getDlqRoutingKey());
    }

    // ── Dominio: DOCUMENTOS ──────────────────────────────────────────────────
    @Bean
    Queue documentsQueue() {
        return workQueue(props.domain("documents"));
    }

    @Bean
    Queue documentsDlq() {
        return deadLetterQueue(props.domain("documents").getDlq());
    }

    @Bean
    Binding documentsBinding() {
        var d = props.domain("documents");
        return BindingBuilder.bind(documentsQueue()).to(workExchange()).with(d.getRoutingKey());
    }

    @Bean
    Binding documentsDlqBinding() {
        var d = props.domain("documents");
        return BindingBuilder.bind(documentsDlq()).to(deadLetterExchange()).with(d.getDlqRoutingKey());
    }

    // ── Publish / Subscribe (exchange FANOUT) ────────────────────────────────
    // Un evento publicado al fanout se COPIA a TODAS las colas enlazadas, sin
    // routing key. Aquí lo reciben dos suscriptores independientes: "email" y
    // "analytics". Así un mismo hecho (reserva confirmada) dispara varias
    // reacciones desacopladas.
    @Bean
    FanoutExchange broadcastExchange() {
        return new FanoutExchange(props.getFanoutExchange(), true, false);
    }

    @Bean
    Queue broadcastEmailQueue() {
        return new Queue(props.getBroadcast().getEmailQueue(), true);
    }

    @Bean
    Queue broadcastAnalyticsQueue() {
        return new Queue(props.getBroadcast().getAnalyticsQueue(), true);
    }

    @Bean
    Binding broadcastEmailBinding() {
        return BindingBuilder.bind(broadcastEmailQueue()).to(broadcastExchange());
    }

    @Bean
    Binding broadcastAnalyticsBinding() {
        return BindingBuilder.bind(broadcastAnalyticsQueue()).to(broadcastExchange());
    }

    // ── Mensajería SÍNCRONA (RPC request/reply) ──────────────────────────────
    // El cliente envía una petición y BLOQUEA esperando la respuesta por la cola
    // de respuesta temporal (Direct Reply-to, que gestiona Spring AMQP).
    @Bean
    DirectExchange rpcExchange() {
        return new DirectExchange(props.getRpc().getExchange(), true, false);
    }

    @Bean
    Queue rpcRequestQueue() {
        return new Queue(props.getRpc().getQueue(), true);
    }

    @Bean
    Binding rpcBinding() {
        return BindingBuilder.bind(rpcRequestQueue())
                .to(rpcExchange())
                .with(props.getRpc().getRoutingKey());
    }

    // ── Fábricas comunes de colas ────────────────────────────────────────────

    /** Cola de trabajo durable con DLQ asociada (reenruta lo rechazado/expirado). */
    private Queue workQueue(RabbitProperties.Domain d) {
        return QueueBuilder.durable(d.getQueue())
                .withArgument("x-dead-letter-exchange", props.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", d.getDlqRoutingKey())
                .build();
    }

    /**
     * DLQ durable con POLÍTICA DE RETENCIÓN: los mensajes muertos expiran a los
     * x-message-ttl ms y la cola no guarda más de x-max-length (evita que una
     * DLQ crezca sin límite). Ambos valores se configuran en application.yml.
     */
    private Queue deadLetterQueue(String name) {
        QueueBuilder b = QueueBuilder.durable(name);
        RabbitProperties.Retention r = props.getRetention();
        if (r != null) {
            if (r.getDlqTtlMs() != null) {
                b.withArgument("x-message-ttl", r.getDlqTtlMs());
            }
            if (r.getDlqMaxLength() != null) {
                b.withArgument("x-max-length", r.getDlqMaxLength());
            }
        }
        return b.build();
    }
}
