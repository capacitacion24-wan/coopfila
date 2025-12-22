package com.example.ticketero.model.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ClienteRequest(
    @NotBlank(message = "National ID is required")
    @Size(max = 20, message = "National ID max 20 characters")
    String nationalId,

    @NotBlank(message = "Nombre is required")
    @Size(min = 2, max = 100, message = "Nombre must be 2-100 characters")
    String nombre,

    @NotBlank(message = "Apellido is required")
    @Size(min = 2, max = 100, message = "Apellido must be 2-100 characters")
    String apellido,

    @Size(max = 20, message = "Telefono max 20 characters")
    String telefono,

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email max 100 characters")
    String email,

    @Past(message = "Fecha nacimiento must be in the past")
    LocalDate fechaNacimiento
) {}