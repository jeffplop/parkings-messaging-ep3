package cl.duoc.parkings.messaging.admin.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas básicas de la validación de DTOs (rúbrica indicador 8). Confirman que
 * no se pueden crear colas/exchanges con nombres vacíos o configuraciones
 * inválidas, sin necesidad de levantar el servidor web.
 */
class AdminDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        // ParameterMessageInterpolator evita depender de una implementación de EL.
        factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) factory.close();
    }

    @Test
    @DisplayName("QueueRequest válido no genera violaciones")
    void colaValida() {
        assertThat(validator.validate(new QueueRequest("parkings.demo.q", true))).isEmpty();
    }

    @Test
    @DisplayName("QueueRequest con nombre vacío es rechazado")
    void colaNombreVacioRechazada() {
        assertThat(validator.validate(new QueueRequest("", true))).isNotEmpty();
    }

    @Test
    @DisplayName("QueueRequest con caracteres inválidos es rechazado")
    void colaNombreConEspaciosRechazada() {
        assertThat(validator.validate(new QueueRequest("cola invalida!", true))).isNotEmpty();
    }

    @Test
    @DisplayName("QueueRequest sin 'durable' toma TRUE por defecto")
    void colaDurablePorDefecto() {
        assertThat(new QueueRequest("parkings.demo.q", null).durable()).isTrue();
    }

    @Test
    @DisplayName("ExchangeRequest con tipo no permitido es rechazado")
    void exchangeTipoInvalidoRechazado() {
        assertThat(validator.validate(new ExchangeRequest("parkings.ex", "invalido", true))).isNotEmpty();
    }

    @Test
    @DisplayName("ExchangeRequest tipo topic es válido")
    void exchangeTopicValido() {
        assertThat(validator.validate(new ExchangeRequest("parkings.ex", "topic", true))).isEmpty();
    }

    @Test
    @DisplayName("BindingRequest con routingKey nula es rechazado; vacía es válida (fanout)")
    void bindingRoutingKey() {
        assertThat(validator.validate(new BindingRequest("q", "ex", null))).isNotEmpty();
        assertThat(validator.validate(new BindingRequest("q", "ex", ""))).isEmpty();
    }
}
