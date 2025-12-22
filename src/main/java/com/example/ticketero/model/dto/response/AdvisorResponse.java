package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;

public record AdvisorResponse(
    Long id,
    String name,
    String email,
    String status,
    Integer moduleNumber,
    Integer assignedTicketsCount,
    LocalDateTime createdAt
) {}