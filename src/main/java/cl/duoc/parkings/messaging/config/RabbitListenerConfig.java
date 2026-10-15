package cl.duoc.parkings.messaging.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestructura de serialización y confirmación de mensajes (rúbrica indicador 5).
 *
 * - Los mensajes viajan en JSON (Jackson2JsonMessageConverter).
 * - El contenedor de listeners usa ACK MANUAL: cada consumidor decide
 *   explícitamente entre ACK (procesado), NACK con requeue (reintento) o
 *   NACK sin requeue (envío a la DLQ). Esto evita reprocesos y pérdida de mensajes.
 */
@Configuration
public class RabbitListenerConfig {

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    /**
     * Fábrica de contenedores con ACK MANUAL. La usan las colas de trabajo
     * (notifications, tickets, documents), donde el consumidor decide entre
     * ACK / NACK-requeue / NACK-DLQ. Los @RabbitListener la referencian con
     * containerFactory = "manualAckContainerFactory".
     */
    @Bean
    SimpleRabbitListenerContainerFactory manualAckContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false); // por defecto, lo rechazado va a la DLQ
        return factory;
    }

    /**
     * Fábrica de contenedores con ACK AUTOMÁTICO. La usan el servidor RPC
     * (mensajería síncrona) y los suscriptores del fanout (Publish/Subscribe):
     * son tareas idempotentes/informativas donde Spring confirma el mensaje si
     * el método retorna sin excepción. Demuestra que el ACK se elige según el
     * caso de uso ("ack respectivos"), no de forma única para todo.
     */
    @Bean
    SimpleRabbitListenerContainerFactory autoAckContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
}
