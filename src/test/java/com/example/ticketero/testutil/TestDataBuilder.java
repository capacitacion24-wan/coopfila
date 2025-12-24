package com.example.ticketero.testutil;

import com.example.ticketero.model.dto.request.ClienteRequest;
import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.entity.*;
import com.example.ticketero.model.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builder para crear datos de prueba consistentes.
 */
public class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class - prevent instantiation
    }

    // ============================================================
    // TICKETS
    // ============================================================
    
    public static Ticket.TicketBuilder ticketEnEspera() {
        return Ticket.builder()
            .id(1L)
            .codigoReferencia(UUID.randomUUID())
            .numero("C01")
            .branchOffice("Sucursal Centro")
            .queueType(QueueType.CAJA)
            .status(TicketStatus.EN_ESPERA)
            .positionInQueue(1)
            .estimatedWaitMinutes(5)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now());
    }
    
    public static Ticket.TicketBuilder ticketAtendiendo() {
        return ticketEnEspera()
            .status(TicketStatus.ATENDIENDO);
    }
    
    public static Ticket.TicketBuilder ticketCompletado() {
        return ticketAtendiendo()
            .status(TicketStatus.COMPLETADO);
    }

    // ============================================================
    // CLIENTES
    // ============================================================
    
    public static Cliente.ClienteBuilder clienteValido() {
        return Cliente.builder()
            .id(1L)
            .nationalId("12345678")
            .nombre("Juan")
            .apellido("Pérez")
            .telefono("+56912345678")
            .email("juan.perez@email.com")
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .createdAt(LocalDateTime.now());
    }

    // ============================================================
    // ADVISORS
    // ============================================================
    
    public static Advisor.AdvisorBuilder advisorDisponible() {
        return Advisor.builder()
            .id(1L)
            .name("María López")
            .email("maria.lopez@banco.com")
            .status(AdvisorStatus.AVAILABLE)
            .moduleNumber(1)
            .assignedTicketsCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now());
    }
    
    public static Advisor.AdvisorBuilder advisorOcupado() {
        return advisorDisponible()
            .status(AdvisorStatus.BUSY)
            .assignedTicketsCount(1);
    }

    // ============================================================
    // MENSAJES
    // ============================================================
    
    public static Mensaje.MensajeBuilder mensajePendiente() {
        return Mensaje.builder()
            .id(1L)
            .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
            .estadoEnvio(MessageStatus.PENDIENTE)
            .fechaProgramada(LocalDateTime.now())
            .intentos(0)
            .createdAt(LocalDateTime.now());
    }

    // ============================================================
    // REQUESTS
    // ============================================================
    
    public static TicketRequest ticketRequestValido() {
        return new TicketRequest(
            1L,
            "Sucursal Centro",
            QueueType.CAJA
        );
    }
    
    public static ClienteRequest clienteRequestValido() {
        return new ClienteRequest(
            "12345678",
            "Juan",
            "Pérez",
            "+56912345678",
            "juan.perez@email.com",
            LocalDate.of(1990, 1, 1)
        );
    }

    // ============================================================
    // AUDIT LOGS
    // ============================================================
    
    public static AuditLog.AuditLogBuilder auditLogValido() {
        return AuditLog.builder()
            .id(1L)
            .tipoEvento("TICKET_CREADO")
            .actor("SYSTEM")
            .entityType("Ticket")
            .entityId("1")
            .timestamp(LocalDateTime.now());
    }
}