package cl.duoc.parkings.messaging.event;

/** Solicitud asíncrona de generación de un documento (factura, guía, PDF...). */
public record DocumentEvent(
        String reservaId,
        String tipo,      // FACTURA | GUIA | COMPROBANTE
        String formato    // PDF | XML
) {}
