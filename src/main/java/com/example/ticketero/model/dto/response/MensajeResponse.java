package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;

public record MensajeResponse(
    Long id,
    Long ticketId,
    String plantilla,
    String estadoEnvio,
    LocalDateTime fechaProgramada,
    LocalDateTime fechaEnvio,
    String telegramMessageId,
    Integer intentos,
    LocalDateTime createdAt
) {}