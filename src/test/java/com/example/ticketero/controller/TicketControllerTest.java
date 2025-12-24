package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@DisplayName("TicketController - Web Layer Tests")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @Nested
    @DisplayName("POST /api/tickets")
    class CrearTicket {

        @Test
        @DisplayName("con datos válidos → debe retornar 201 Created")
        void create_conDatosValidos_debeRetornar201() throws Exception {
            // Given
            TicketRequest request = new TicketRequest(1L, "Sucursal Centro", QueueType.CAJA);
            TicketResponse response = new TicketResponse(
                1L,
                UUID.randomUUID(),
                "C001",
                1L,
                "Juan Pérez",
                "Sucursal Centro",
                "CAJA",
                "EN_ESPERA",
                1,
                5,
                null,
                null,
                null,
                LocalDateTime.now()
            );

            when(ticketService.create(any(TicketRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("C001"))
                .andExpect(jsonPath("$.status").value("EN_ESPERA"))
                .andExpect(jsonPath("$.positionInQueue").value(1))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
        }

        @Test
        @DisplayName("con clienteId null → debe retornar 400 Bad Request")
        void create_conClienteIdNull_debeRetornar400() throws Exception {
            // Given
            TicketRequest request = new TicketRequest(null, "Sucursal Centro", QueueType.CAJA);

            // When & Then
            mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/{codigo}")
    class ObtenerTicket {

        @Test
        @DisplayName("con código existente → debe retornar ticket")
        void getByCodigoReferencia_conCodigoExistente_debeRetornarTicket() throws Exception {
            // Given
            UUID codigo = UUID.randomUUID();
            TicketResponse response = new TicketResponse(
                1L,
                codigo,
                "C001",
                1L,
                "Juan Pérez",
                "Sucursal Centro",
                "CAJA",
                "EN_ESPERA",
                1,
                5,
                null,
                null,
                null,
                LocalDateTime.now()
            );

            when(ticketService.findByCodigoReferencia(codigo)).thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(get("/api/tickets/{codigo}", codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("C001"))
                .andExpect(jsonPath("$.codigoReferencia").value(codigo.toString()));
        }

        @Test
        @DisplayName("con código inexistente → debe retornar 404 Not Found")
        void getByCodigoReferencia_conCodigoInexistente_debeRetornar404() throws Exception {
            // Given
            UUID codigo = UUID.randomUUID();
            when(ticketService.findByCodigoReferencia(codigo)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/tickets/{codigo}", codigo))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/active")
    class TicketsActivos {

        @Test
        @DisplayName("debe retornar lista de tickets activos")
        void getActiveTickets_debeRetornarListaTicketsActivos() throws Exception {
            // Given
            TicketResponse ticket1 = new TicketResponse(
                1L, UUID.randomUUID(), "C001", 1L, "Juan Pérez",
                "Sucursal Centro", "CAJA", "EN_ESPERA", 1, 5, 
                null, null, null, LocalDateTime.now()
            );

            when(ticketService.findActiveTickets()).thenReturn(List.of(ticket1));

            // When & Then
            mockMvc.perform(get("/api/tickets/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].numero").value("C001"));
        }
    }
}