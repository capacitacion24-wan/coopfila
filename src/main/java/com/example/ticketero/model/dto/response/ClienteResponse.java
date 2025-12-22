package com.example.ticketero.model.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponse(
    Long id,
    String nationalId,
    String nombre,
    String apellido,
    String telefono,
    String email,
    LocalDate fechaNacimiento,
    LocalDateTime createdAt
) {}