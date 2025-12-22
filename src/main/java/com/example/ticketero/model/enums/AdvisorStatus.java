package com.example.ticketero.model.enums;

/**
 * Estados posibles de un asesor
 */
public enum AdvisorStatus {
    AVAILABLE,  // Disponible para atender
    BUSY,       // Atendiendo un cliente
    OFFLINE;    // No disponible (almuerzo, capacitación, etc.)

    /**
     * Verifica si el asesor puede recibir asignaciones
     */
    public boolean canReceiveAssignments() {
        return this == AVAILABLE;
    }
}