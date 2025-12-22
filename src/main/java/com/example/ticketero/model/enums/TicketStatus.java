package com.example.ticketero.model.enums;

import java.util.List;

/**
 * Estados posibles de un ticket
 */
public enum TicketStatus {
    EN_ESPERA,      // Esperando asignación
    PROXIMO,        // Próximo a ser atendido (posición <= 3)
    ATENDIENDO,     // Siendo atendido por un asesor
    COMPLETADO,     // Atención finalizada
    CANCELADO,      // Cancelado por cliente o sistema
    NO_ATENDIDO;    // Cliente no se presentó

    /**
     * Estados considerados "activos" (cliente aún no ha sido atendido completamente)
     */
    public static List<TicketStatus> getActiveStatuses() {
        return List.of(EN_ESPERA, PROXIMO, ATENDIENDO);
    }

    /**
     * Verifica si este estado es considerado activo
     */
    public boolean isActive() {
        return getActiveStatuses().contains(this);
    }
}