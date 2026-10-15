package cl.duoc.parkings.messaging.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas básicas de la topología declarada como beans (rúbrica indicadores 1, 2
 * y 5): verifican que la cola de trabajo enlaza su DLX/DLQ y que la DLQ aplica
 * la política de retención (TTL + longitud máxima). No requieren broker.
 */
class RabbitTopologyConfigTest {

    private RabbitTopologyConfig config;

    @BeforeEach
    void setUp() {
        RabbitProperties props = new RabbitProperties();
        props.setExchange("parkings.exchange");
        props.setDeadLetterExchange("parkings.dlx");

        RabbitProperties.Domain tickets = new RabbitProperties.Domain();
        tickets.setQueue("parkings.tickets.q");
        tickets.setDlq("parkings.tickets.dlq");
        tickets.setRoutingKey("ticket.generate");
        tickets.setDlqRoutingKey("dlq.ticket");
        props.getDomains().put("tickets", tickets);
        props.getDomains().put("notifications", tickets); // basta un dominio para estas pruebas
        props.getDomains().put("documents", tickets);

        RabbitProperties.Retention retention = new RabbitProperties.Retention();
        retention.setDlqTtlMs(604800000L);
        retention.setDlqMaxLength(10000);
        props.setRetention(retention);

        config = new RabbitTopologyConfig(props);
    }

    @Test
    @DisplayName("La cola de trabajo apunta al Dead Letter Exchange y su routing key")
    void colaDeTrabajoEnlazaDlx() {
        Queue q = config.ticketsQueue();
        assertThat(q.getArguments()).containsEntry("x-dead-letter-exchange", "parkings.dlx");
        assertThat(q.getArguments()).containsEntry("x-dead-letter-routing-key", "dlq.ticket");
        assertThat(q.isDurable()).isTrue();
    }

    @Test
    @DisplayName("La DLQ aplica política de retención: TTL y longitud máxima")
    void dlqAplicaRetencion() {
        Queue dlq = config.ticketsDlq();
        assertThat(dlq.getArguments()).containsEntry("x-message-ttl", 604800000L);
        assertThat(dlq.getArguments()).containsEntry("x-max-length", 10000);
        assertThat(dlq.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Los exchanges de trabajo y de mensajes muertos son durables")
    void exchangesDurables() {
        TopicExchange work = config.workExchange();
        TopicExchange dlx = config.deadLetterExchange();
        assertThat(work.getName()).isEqualTo("parkings.exchange");
        assertThat(work.isDurable()).isTrue();
        assertThat(dlx.getName()).isEqualTo("parkings.dlx");
        assertThat(dlx.isDurable()).isTrue();
    }

    @Test
    @DisplayName("El binding de trabajo usa la routing key del dominio")
    void bindingDeTrabajo() {
        Binding b = config.ticketsBinding();
        assertThat(b.getRoutingKey()).isEqualTo("ticket.generate");
        assertThat(b.getExchange()).isEqualTo("parkings.exchange");
        assertThat(b.getDestination()).isEqualTo("parkings.tickets.q");
    }
}
