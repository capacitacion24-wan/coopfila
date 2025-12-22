package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.constraints.*;

public record TicketRequest(
    @NotNull(message = "Cliente ID is required")
    Long clienteId,

    @NotBlank(message = "Branch office is required")
    @Size(max = 100, message = "Branch office max 100 characters")
    String branchOffice,

    @NotNull(message = "Queue type is required")
    QueueType queueType
) {}