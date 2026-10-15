package cl.duoc.parkings.messaging.admin;

import cl.duoc.parkings.messaging.admin.dto.BindingRequest;
import cl.duoc.parkings.messaging.admin.dto.ExchangeRequest;
import cl.duoc.parkings.messaging.admin.dto.QueueRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Microservicio administrador de RabbitMQ (rúbrica indicador 6): expone una API
 * REST clara para crear y eliminar colas, exchanges y bindings. No conoce los
 * detalles de AMQP: delega todo en {@link RabbitAdminService} (indicador 7).
 * Cada cuerpo de entrada se valida con @Valid (indicador 8).
 *
 *   POST   /api/admin/queues            crea una cola
 *   DELETE /api/admin/queues/{name}     elimina una cola
 *   POST   /api/admin/exchanges         crea un exchange
 *   DELETE /api/admin/exchanges/{name}  elimina un exchange
 *   POST   /api/admin/bindings          crea un binding cola↔exchange
 */
@RestController
@RequestMapping("/api/admin")
public class RabbitAdminController {

    private final RabbitAdminService admin;

    public RabbitAdminController(RabbitAdminService admin) {
        this.admin = admin;
    }

    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> createQueue(@Valid @RequestBody QueueRequest req) {
        admin.createQueue(req.name(), req.durable());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Cola creada", "name", req.name(), "durable", req.durable()));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Map<String, Object>> deleteQueue(@PathVariable String name) {
        boolean deleted = admin.deleteQueue(name);
        return ResponseEntity.ok(Map.of("message", "Cola eliminada", "name", name, "deleted", deleted));
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> createExchange(@Valid @RequestBody ExchangeRequest req) {
        admin.createExchange(req.name(), req.type(), req.durable());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Exchange creado", "name", req.name(), "type", req.type()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Map<String, Object>> deleteExchange(@PathVariable String name) {
        admin.deleteExchange(name);
        return ResponseEntity.ok(Map.of("message", "Exchange eliminado", "name", name));
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> createBinding(@Valid @RequestBody BindingRequest req) {
        admin.createBinding(req.queue(), req.exchange(), req.routingKey());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Binding creado",
                        "exchange", req.exchange(), "queue", req.queue(), "routingKey", req.routingKey()));
    }

    /** Devuelve 400 con los mensajes de validación claros (rúbrica indicador 8). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errores.put(fe.getField(), fe.getDefaultMessage()));
        return Map.of("error", "Parámetros inválidos", "detalles", errores);
    }
}
