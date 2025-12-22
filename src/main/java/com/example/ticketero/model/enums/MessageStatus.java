package com.example.ticketero.model.enums;

/**
 * Estados posibles de un mensaje
 */
public enum MessageStatus {
    PENDIENTE,  // Esperando ser enviado
    ENVIADO,    // Enviado exitosamente
    FALLIDO;    // Falló el envío (después de reintentos)

    /**
     * Verifica si el mensaje necesita ser procesado
     */
    public boolean needsProcessing() {
        return this == PENDIENTE;
    }
}