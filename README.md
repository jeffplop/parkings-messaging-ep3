# Parkings Together · Mensajería con RabbitMQ (DSY1107 — EP3)

Microservicio **Spring Boot** que incorpora **RabbitMQ** al sistema *Parkings Together*
para procesar tareas asíncronas de forma **desacoplada**: notificaciones a
conductores/arrendadores, generación de **tickets** (comprobante de pago de una
reserva) y **documentos** (factura/comprobante). Incluye **colas, exchanges,
DLQ** y un **microservicio administrador** de la topología vía REST.

> Caso semestral: **Parkings Together**. Como DSY1107 exige **Java + Spring Boot
> + Angular**, esta capa de mensajería se implementa en Spring Boot (la rúbrica
> evalúa `@RabbitListener`, `@Configuration`, beans de `Queue/Exchange/Binding`,
> ACK/DLQ, etc.). No modifica el frontend Next.js del producto; se integra con el
> backend de microservicios del ramo.

---

## 🏛️ Arquitectura de mensajería

```
                 publish (convertAndSend)
  Negocio ───────────────────────────────►  parkings.exchange  (topic)
  (MessagePublisher)                              │
                         routing keys             │
        notification.created / ticket.generate / document.generate
                                                  ▼
         ┌───────────────────────┬───────────────────────┬───────────────────────┐
         │ parkings.notifications.q │ parkings.tickets.q   │ parkings.documents.q  │
         └───────────┬───────────┴───────────┬───────────┴───────────┬───────────┘
            @RabbitListener           @RabbitListener           @RabbitListener
         NotificationConsumer        TicketConsumer           DocumentConsumer
                     │   ACK / NACK(requeue) / NACK(→DLQ)  │
                     ▼                                     ▼
            (x-dead-letter-exchange = parkings.dlx, por cola)
                                      ▼
                             parkings.dlx (topic)
                                      ▼
         parkings.*.dlq  ──►  DeadLetterConsumer  (registra en logs)
```

- **1 exchange de trabajo** (topic) + **1 Dead Letter Exchange** (DLX).
- **3 dominios** (notifications, tickets, documents); cada uno con su **cola** y su **DLQ**.
- Un mensaje que se rechaza sin requeue (o que falla el reintento) se **re-enruta a la DLQ** vía el DLX y queda **registrado en logs**.

---

## ✅ Mapeo a la pauta de evaluación

| # | Indicador (rúbrica) | % | Dónde |
|---|---|---|---|
| 1 | Nombres de colas/exchanges/bindings **centralizados** | 12 | `application.yml` (`parkings.rabbit.*`) + `config/RabbitProperties.java` |
| 2 | **Beans** `Queue`/`Exchange`/`Binding` por caso de uso | 13 | `config/RabbitTopologyConfig.java` |
| 3 | **No mezclar** negocio con config de mensajería | 10 | Config aislada en `config/*`; negocio en `consumer/*` y `producer/*` |
| 4 | Consumidores con **`@RabbitListener`** agrupados por dominio | 15 | `consumer/NotificationConsumer`, `TicketConsumer`, `DocumentConsumer` |
| 5 | **ACK manual** + errores (reintento/NACK/**DLQ**) | 20 | `config/RabbitListenerConfig.java` + los 3 consumidores + `DeadLetterConsumer` |
| 6 | Microservicio **admin** con REST | 13 | `admin/RabbitAdminController.java` |
| 7 | Lógica de administración **encapsulada en un servicio** | 10 | `admin/RabbitAdminService.java` |
| 8 | **Validación** de parámetros (DTO) | 7 | `admin/dto/*Request.java` (`@NotBlank`, `@Pattern`, …) |

Extra (instrucciones): **JWT del IDaaS** → `security/SecurityConfig.java`; `.gitignore`
configurado; separación clara de paquetes por responsabilidad.

---

## 🎯 Checklist "Fundamental para la Prueba 2" (19-10-2026)

Lo pedido en clase (sábado 10-10-2026) y dónde está implementado:

| Concepto | Qué es aquí | Dónde | Cómo se demuestra |
|---|---|---|---|
| **Mensajes asíncronos** | publicar y seguir sin esperar (fire-and-forget) | `producer/MessagePublisher` + `consumer/*Consumer` | `POST /api/demo/tickets` → responde 202 y procesa en segundo plano |
| **Mensajes síncronos** | RPC: envío y **espero** la respuesta | `producer/SyncMessageClient` + `consumer/QuoteRpcServer` | `POST /api/demo/cotizar` → responde 200 con el total ya calculado |
| **Ack respectivos** | ACK manual (colas de trabajo) y ACK auto (RPC/pub-sub) | `config/RabbitListenerConfig` (2 fábricas) | logs `[tickets] ACK ...` / NACK / DLQ |
| **Durabilidad** | colas/exchanges durables + mensajes PERSISTENT | `config/RabbitTopologyConfig` + `MessagePublisher#persistente` | sobreviven a reinicio del broker (quedan en disco) |
| **Publish / Subscribe** | 1 evento → N suscriptores (exchange **fanout**) | `broadcastExchange` + `consumer/BroadcastSubscribers` | `POST /api/demo/broadcast` → logs `[sub:email]` **y** `[sub:analytics]` |
| **DLX / DLQ** | Dead Letter Exchange + 1 DLQ por dominio | `RabbitTopologyConfig` (`x-dead-letter-exchange`) + `DeadLetterConsumer` | mensaje inválido → DLQ |
| **Políticas de retención** | TTL + longitud máx. en las DLQ | `RabbitTopologyConfig#deadLetterQueue` (`x-message-ttl`, `x-max-length`) | visible en el panel (features `TTL`, `Lim`) |
| **Alertas** | aviso al superar un umbral de mensajes muertos | `DeadLetterConsumer` (log ERROR `⚠️/🚨`) | logs de alerta al 5.º muerto |

> **Síncrono vs. asíncrono (resumen):** el asíncrono usa `convertAndSend` (no espera);
> el síncrono usa `convertSendAndReceive` (bloquea hasta recibir la respuesta por
> *Direct Reply-to*). El mismo broker sirve ambos estilos.

---

## 📁 Estructura

```
parkings-messaging/
├─ pom.xml
├─ docker-compose.yml           # RabbitMQ local (puertos 5672 y 15672)
├─ .gitignore
├─ src/test/java/...            # pruebas básicas (JUnit 5): RPC, validación DTO, topología/DLQ
└─ src/main/
   ├─ resources/application.yml  # topología centralizada + conexión + IDaaS
   └─ java/cl/duoc/parkings/messaging/
      ├─ MessagingApplication.java
      ├─ config/   RabbitProperties · RabbitTopologyConfig · RabbitListenerConfig
      ├─ event/    NotificationEvent · TicketEvent · DocumentEvent
      │            QuoteRequest · QuoteResponse (RPC síncrono) · ReservaConfirmadaEvent (pub/sub)
      ├─ producer/ MessagePublisher (asíncrono + fanout) · SyncMessageClient (RPC síncrono)
      ├─ consumer/ NotificationConsumer · TicketConsumer · DocumentConsumer · DeadLetterConsumer · TransientProcessingException
      │            QuoteRpcServer (servidor RPC) · BroadcastSubscribers (suscriptores pub/sub)
      ├─ admin/    RabbitAdminController · RabbitAdminService · dto/(QueueRequest · ExchangeRequest · BindingRequest)
      ├─ security/ SecurityConfig
      └─ demo/     DemoPublishController   (solo para demostrar; dispara publicaciones)
```

---

## ▶️ Cómo ejecutar y demostrar

**1) Levanta RabbitMQ** (Docker):
```bash
docker compose up -d
# Panel: http://localhost:15672  (guest / guest)
```

**2) Arranca el microservicio** en perfil `local` (sin IDaaS, para demo):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Al arrancar, Spring declara automáticamente los exchanges, las colas y las DLQ
(visibles en el panel de RabbitMQ).

**3) Publica mensajes** (demo) y observa el ACK en los logs:
```bash
curl -X POST http://localhost:8083/api/demo/notifications -H "Content-Type: application/json" \
  -d '{"id":"N1","destinatario":"conductor@correo.cl","canal":"EMAIL","asunto":"Reserva confirmada","mensaje":"Tu reserva fue confirmada"}'

curl -X POST http://localhost:8083/api/demo/tickets -H "Content-Type: application/json" \
  -d '{"reservaId":"R-1001","cliente":"Jefferson","monto":3500}'
```

**4) Fuerza una DLQ** (envía un mensaje inválido → el consumidor lo manda a la DLQ y el `DeadLetterConsumer` lo registra):
```bash
curl -X POST http://localhost:8083/api/demo/tickets -H "Content-Type: application/json" \
  -d '{"reservaId":"","cliente":"X","monto":0}'
# En logs: "[tickets] error NO recuperable → DLQ" y luego "⚠️ [ALERTA-DLQ] Mensaje muerto ..."
```

**4b) Publish/Subscribe** (un evento → varios suscriptores a la vez):
```bash
curl -X POST http://localhost:8083/api/demo/broadcast -H "Content-Type: application/json" \
  -d '{"reservaId":"R-2002","cliente":"Jefferson","patente":"ABCD12","instante":"2026-10-19T10:00:00"}'
# En logs aparecen AMBOS: "[sub:email] Enviando correo ..." y "[sub:analytics] Registrando métrica ..."
```

**4c) Mensajería SÍNCRONA (RPC)** (envío y recibo la respuesta en el mismo request):
```bash
curl -X POST http://localhost:8083/api/demo/cotizar -H "Content-Type: application/json" \
  -d '{"reservaId":"R-3003","zona":"CENTRO","horas":3}'
# Responde 200: {"reservaId":"R-3003","tarifaHora":1800,"total":5400,"moneda":"CLP"}
```

**5) Administra la topología** (microservicio admin):
```bash
# Crear una cola
curl -X POST http://localhost:8083/api/admin/queues -H "Content-Type: application/json" \
  -d '{"name":"parkings.demo.q","durable":true}'

# Crear un exchange y un binding
curl -X POST http://localhost:8083/api/admin/exchanges -H "Content-Type: application/json" \
  -d '{"name":"parkings.demo.ex","type":"topic","durable":true}'
curl -X POST http://localhost:8083/api/admin/bindings -H "Content-Type: application/json" \
  -d '{"queue":"parkings.demo.q","exchange":"parkings.demo.ex","routingKey":"demo.*"}'

# Eliminar la cola
curl -X DELETE http://localhost:8083/api/admin/queues/parkings.demo.q

# Validación (nombre vacío) → 400 con mensaje claro
curl -X POST http://localhost:8083/api/admin/queues -H "Content-Type: application/json" -d '{"name":""}'
```

---

## 🧪 Pruebas básicas

El enunciado exige que el backend *"compile y responda a pruebas básicas"*. Se
incluyen **15 pruebas JUnit 5** que corren **sin broker**:

```bash
mvn test
```

| Clase de prueba | Qué verifica | Indicador |
|---|---|---|
| `consumer/QuoteRpcServerTest` | cálculo de la cotización síncrona (RPC) | 4 |
| `admin/dto/AdminDtoValidationTest` | no se crean colas/exchanges inválidos | 8 |
| `config/RabbitTopologyConfigTest` | la cola enlaza su DLX/DLQ y la DLQ aplica retención | 1, 2, 5 |

---

## 🔐 En producción (con IDaaS)

Arranca **sin** el perfil `local` y define las variables del IDaaS:
```bash
export IDAAS_ISSUER_URI=https://<tu-idaas>/realms/parkings
export IDAAS_JWK_SET_URI=https://<tu-idaas>/realms/parkings/protocol/openid-connect/certs
export RABBITMQ_HOST=... RABBITMQ_USER=... RABBITMQ_PASS=...
mvn spring-boot:run
```
Toda petición exige `Authorization: Bearer <JWT>`. El **frontend Angular** obtiene
ese JWT en el login con IDaaS y lo adjunta con un interceptor:

```ts
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private oauth: OAuthService) {}
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = this.oauth.getAccessToken();           // JWT emitido por el IDaaS
    const authReq = token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;
    return next.handle(authReq);
  }
}
```

---

## 🧩 Relación con "varios microservicios"

Este repositorio es el **microservicio de mensajería + administrador**. En el
backend del ramo conviven además los microservicios de negocio (p. ej.
*reservas* y *pagos*), que **publican** eventos usando la misma topología
centralizada. El patrón de `config/`, `producer/` y `consumer/` está pensado para
copiarse/compartirse entre ellos sin duplicar nombres (todo en `application.yml`).

## 📤 Entrega
Subir este proyecto a un repositorio **GitHub** (backend) y el componente
**Angular** a otro, y enviar ambos enlaces por AVA + correo al docente.
