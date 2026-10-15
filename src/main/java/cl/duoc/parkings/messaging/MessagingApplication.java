package cl.duoc.parkings.messaging;

import cl.duoc.parkings.messaging.config.RabbitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio de mensajería de Parkings Together (DSY1107 — EP3).
 *
 * Integra RabbitMQ para tareas asíncronas (notificaciones, tickets, documentos)
 * de forma desacoplada: publica y consume mensajes sin acoplar la lógica de
 * negocio a la infraestructura de colas. Incluye además el microservicio
 * administrador de topología (colas, exchanges y bindings) vía REST.
 */
@SpringBootApplication
@EnableConfigurationProperties(RabbitProperties.class)
public class MessagingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingApplication.class, args);
    }
}
