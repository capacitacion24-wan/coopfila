package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    Long id,
    UUID codigoReferencia,
    String numero,
    Long clienteId,
    String clienteNombre,
    String branchOffice,
    String queueType,
    String status,
    Integer positionInQueue,
    Integer estimatedWaitMinutes,
    Long assignedAdvisorId,
    String assignedAdvisorName,
    Integer assignedModuleNumber,
    LocalDateTime createdAt
) {}