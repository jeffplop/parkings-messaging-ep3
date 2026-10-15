package cl.duoc.parkings.messaging.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cola (rúbrica indicador 8: validación de entrada).
 * No se permiten nombres vacíos ni con caracteres inválidos.
 */
public record QueueRequest(

        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._\\-:]+$",
                 message = "El nombre solo admite letras, números y . _ - :")
        String name,

        Boolean durable
) {
    public QueueRequest {
        if (durable == null) durable = Boolean.TRUE; // por defecto, durable
    }
}
