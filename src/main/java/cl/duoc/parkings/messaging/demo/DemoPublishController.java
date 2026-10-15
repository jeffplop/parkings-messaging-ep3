package cl.duoc.parkings.messaging.demo;

import cl.duoc.parkings.messaging.event.DocumentEvent;
import cl.duoc.parkings.messaging.event.NotificationEvent;
import cl.duoc.parkings.messaging.event.QuoteRequest;
import cl.duoc.parkings.messaging.event.QuoteResponse;
import cl.duoc.parkings.messaging.event.ReservaConfirmadaEvent;
import cl.duoc.parkings.messaging.event.TicketEvent;
import cl.duoc.parkings.messaging.producer.MessagePublisher;
import cl.duoc.parkings.messaging.producer.SyncMessageClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de DEMOSTRACIÓN: dispara la publicación de mensajes para evidenciar
 * que la mensajería asíncrona funciona (el examen pide "demostrar que la
 * mensajería desacoplada funciona correctamente"). En el sistema real, estas
 * publicaciones las hace la lógica de negocio (p. ej. al confirmarse un pago de
 * reserva), no un endpoint. Se mantiene aparte, en el paquete `demo`.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoPublishController {

    private final MessagePublisher publisher;
    private final SyncMessageClient syncClient;

    public DemoPublishController(MessagePublisher publisher, SyncMessageClient syncClient) {
        this.publisher = publisher;
        this.syncClient = syncClient;
    }

    @PostMapping("/notifications")
    public ResponseEntity<Map<String, String>> notify(@RequestBody NotificationEvent event) {
        publisher.publishNotification(event);
        return ResponseEntity.accepted().body(Map.of("status", "encolado", "dominio", "notifications"));
    }

    @PostMapping("/tickets")
    public ResponseEntity<Map<String, String>> ticket(@RequestBody TicketEvent event) {
        publisher.publishTicket(event);
        return ResponseEntity.accepted().body(Map.of("status", "encolado", "dominio", "tickets"));
    }

    @PostMapping("/documents")
    public ResponseEntity<Map<String, String>> document(@RequestBody DocumentEvent event) {
        publisher.publishDocument(event);
        return ResponseEntity.accepted().body(Map.of("status", "encolado", "dominio", "documents"));
    }

    /**
     * Publish/Subscribe: difunde "reserva confirmada" al fanout. La respuesta es
     * 202 (asíncrono): los suscriptores (email, analytics) reaccionan por su cuenta.
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcast(@RequestBody ReservaConfirmadaEvent event) {
        publisher.publishReservaConfirmada(event);
        return ResponseEntity.accepted().body(Map.of("status", "difundido", "patron", "publish-subscribe"));
    }

    /**
     * Mensajería SÍNCRONA (RPC): envía la cotización y DEVUELVE la respuesta ya
     * calculada en el mismo request HTTP (200), porque el emisor esperó la reply.
     */
    @PostMapping("/cotizar")
    public ResponseEntity<QuoteResponse> cotizar(@RequestBody QuoteRequest request) {
        return ResponseEntity.ok(syncClient.cotizar(request));
    }
}
