package cl.duoc.parkings.messaging.event;

/** Mensaje de notificación a enviar de forma asíncrona (correo, SMS, push...). */
public record NotificationEvent(
        String id,
        String destinatario,
        String canal,     // EMAIL | SMS | PUSH
        String asunto,
        String mensaje
) {}
