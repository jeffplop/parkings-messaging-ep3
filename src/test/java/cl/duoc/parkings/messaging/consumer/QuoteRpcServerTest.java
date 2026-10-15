package cl.duoc.parkings.messaging.consumer;

import cl.duoc.parkings.messaging.event.QuoteRequest;
import cl.duoc.parkings.messaging.event.QuoteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas básicas de la lógica del servidor RPC (mensajería síncrona). No
 * requieren un broker RabbitMQ: validan el cálculo de la cotización.
 */
class QuoteRpcServerTest {

    private final QuoteRpcServer server = new QuoteRpcServer();

    @Test
    @DisplayName("Cotiza zona CENTRO: total = tarifa por hora * horas")
    void cotizaZonaConocida() {
        QuoteResponse r = server.cotizar(new QuoteRequest("R-1", "CENTRO", 3));

        assertThat(r.reservaId()).isEqualTo("R-1");
        assertThat(r.tarifaHora()).isEqualByComparingTo("1800");
        assertThat(r.total()).isEqualByComparingTo("5400"); // 1800 * 3
        assertThat(r.moneda()).isEqualTo("CLP");
    }

    @Test
    @DisplayName("La zona es insensible a mayúsculas/minúsculas")
    void zonaInsensibleAMayusculas() {
        QuoteResponse r = server.cotizar(new QuoteRequest("R-2", "aeropuerto", 2));
        assertThat(r.tarifaHora()).isEqualByComparingTo("2500");
        assertThat(r.total()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Zona desconocida usa la tarifa por defecto")
    void zonaDesconocidaUsaTarifaDefecto() {
        QuoteResponse r = server.cotizar(new QuoteRequest("R-3", "MARTE", 1));
        assertThat(r.tarifaHora()).isEqualByComparingTo("1200");
        assertThat(r.total()).isEqualByComparingTo("1200");
    }

    @Test
    @DisplayName("Horas no positivas lanzan error (petición inválida)")
    void horasInvalidasLanzanError() {
        assertThatThrownBy(() -> server.cotizar(new QuoteRequest("R-4", "CENTRO", 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
