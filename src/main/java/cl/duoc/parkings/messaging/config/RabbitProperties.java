package cl.duoc.parkings.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración CENTRALIZADA de la topología de RabbitMQ (rúbrica indicador 1).
 *
 * Enlaza la sección {@code parkings.rabbit.*} de application.yml. Todos los
 * nombres de exchanges, colas, DLQs y routing-keys viven aquí; el resto del
 * código los referencia por propiedad y nunca con literales sueltos.
 *
 * Cubre además los requisitos "Fundamentales para la Prueba 2":
 *   - {@link #fanoutExchange} + {@link #broadcast} → Publish/Subscribe.
 *   - {@link #rpc} → mensajería SÍNCRONA (request/reply).
 *   - {@link #retention} → políticas de retención (TTL y longitud máxima).
 */
@ConfigurationProperties(prefix = "parkings.rabbit")
public class RabbitProperties {

    /** Exchange de trabajo (tipo topic) para mensajería asíncrona dirigida. */
    private String exchange;

    /** Exchange de mensajes muertos (Dead Letter Exchange). */
    private String deadLetterExchange;

    /** Exchange de difusión (tipo fanout) para Publish/Subscribe. */
    private String fanoutExchange;

    /** Parámetros del canal síncrono (RPC request/reply). */
    private Rpc rpc = new Rpc();

    /** Colas suscriptoras del exchange de difusión (Publish/Subscribe). */
    private Broadcast broadcast = new Broadcast();

    /** Políticas de retención aplicadas a las DLQ. */
    private Retention retention = new Retention();

    /** Un conjunto de nombres por cada dominio funcional (notifications, tickets, documents...). */
    private Map<String, Domain> domains = new LinkedHashMap<>();

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getDeadLetterExchange() { return deadLetterExchange; }
    public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }

    public String getFanoutExchange() { return fanoutExchange; }
    public void setFanoutExchange(String fanoutExchange) { this.fanoutExchange = fanoutExchange; }

    public Rpc getRpc() { return rpc; }
    public void setRpc(Rpc rpc) { this.rpc = rpc; }

    public Broadcast getBroadcast() { return broadcast; }
    public void setBroadcast(Broadcast broadcast) { this.broadcast = broadcast; }

    public Retention getRetention() { return retention; }
    public void setRetention(Retention retention) { this.retention = retention; }

    public Map<String, Domain> getDomains() { return domains; }
    public void setDomains(Map<String, Domain> domains) { this.domains = domains; }

    /** Acceso cómodo y seguro a un dominio concreto. */
    public Domain domain(String name) {
        Domain d = domains.get(name);
        if (d == null) {
            throw new IllegalStateException("No hay configuración de RabbitMQ para el dominio '" + name + "'");
        }
        return d;
    }

    /** Nombres de la cola de trabajo y su DLQ para un dominio funcional. */
    public static class Domain {
        private String queue;          // cola de trabajo
        private String dlq;            // dead letter queue
        private String routingKey;     // routing key de trabajo
        private String dlqRoutingKey;  // routing key hacia la DLQ

        public String getQueue() { return queue; }
        public void setQueue(String queue) { this.queue = queue; }

        public String getDlq() { return dlq; }
        public void setDlq(String dlq) { this.dlq = dlq; }

        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

        public String getDlqRoutingKey() { return dlqRoutingKey; }
        public void setDlqRoutingKey(String dlqRoutingKey) { this.dlqRoutingKey = dlqRoutingKey; }
    }

    /** Canal síncrono (RPC): el cliente envía y ESPERA la respuesta. */
    public static class Rpc {
        private String exchange;    // exchange directo del RPC
        private String routingKey;  // routing key de la petición
        private String queue;       // cola donde escucha el servidor RPC

        public String getExchange() { return exchange; }
        public void setExchange(String exchange) { this.exchange = exchange; }

        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

        public String getQueue() { return queue; }
        public void setQueue(String queue) { this.queue = queue; }
    }

    /** Colas suscriptoras del fanout (Publish/Subscribe): cada una recibe una copia. */
    public static class Broadcast {
        private String emailQueue;      // suscriptor 1: envío de correos
        private String analyticsQueue;  // suscriptor 2: analítica/estadísticas

        public String getEmailQueue() { return emailQueue; }
        public void setEmailQueue(String emailQueue) { this.emailQueue = emailQueue; }

        public String getAnalyticsQueue() { return analyticsQueue; }
        public void setAnalyticsQueue(String analyticsQueue) { this.analyticsQueue = analyticsQueue; }
    }

    /** Política de retención de mensajes en las DLQ. */
    public static class Retention {
        /** Tiempo de vida de un mensaje muerto antes de descartarse (ms). x-message-ttl */
        private Long dlqTtlMs;
        /** Número máximo de mensajes muertos retenidos por DLQ. x-max-length */
        private Integer dlqMaxLength;

        public Long getDlqTtlMs() { return dlqTtlMs; }
        public void setDlqTtlMs(Long dlqTtlMs) { this.dlqTtlMs = dlqTtlMs; }

        public Integer getDlqMaxLength() { return dlqMaxLength; }
        public void setDlqMaxLength(Integer dlqMaxLength) { this.dlqMaxLength = dlqMaxLength; }
    }
}
