package cl.duoc.parkings.messaging.consumer;

/**
 * Error RECUPERABLE (p. ej. un servicio externo caído momentáneamente): justifica
 * un reintento. Un error de otro tipo se considera NO recuperable y el mensaje
 * se envía directamente a la DLQ.
 */
public class TransientProcessingException extends RuntimeException {
    public TransientProcessingException(String message) {
        super(message);
    }
}
