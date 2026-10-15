package cl.duoc.parkings.messaging.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos para crear un exchange (rúbrica indicador 8: validación de entrada).
 */
public record ExchangeRequest(

        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._\\-:]+$",
                 message = "El nombre solo admite letras, números y . _ - :")
        String name,

        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "^(direct|topic|fanout|headers)$",
                 message = "El tipo debe ser direct, topic, fanout o headers")
        String type,

        Boolean durable
) {
    public ExchangeRequest {
        if (durable == null) durable = Boolean.TRUE;
    }
}
