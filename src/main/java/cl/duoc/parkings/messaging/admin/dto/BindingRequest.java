package cl.duoc.parkings.messaging.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear un binding cola↔exchange (rúbrica indicador 8).
 * routingKey puede ser "" (p. ej. fanout), pero no nulo.
 */
public record BindingRequest(

        @NotBlank(message = "La cola es obligatoria")
        String queue,

        @NotBlank(message = "El exchange es obligatorio")
        String exchange,

        @NotNull(message = "La routing key es obligatoria (puede ser cadena vacía)")
        String routingKey
) {}
