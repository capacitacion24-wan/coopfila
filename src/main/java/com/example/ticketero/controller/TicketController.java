package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.ClienteRequest;
import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.ClienteResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.service.AuditService;
import com.example.ticketero.service.ClienteService;
import com.example.ticketero.service.MensajeService;
import com.example.ticketero.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;
    private final ClienteService clienteService;
    private final MensajeService mensajeService;
    private final AuditService auditService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketRequest request) {
        log.info("Creating ticket for cliente: {}", request.clienteId());
        
        TicketResponse ticket = ticketService.create(request);
        
        // TODO: Programar mensajes automáticamente
        
        // Auditar
        auditService.logTicketCreated(ticket.id(), ticket.clienteNombre());
        
        return ResponseEntity.status(201).body(ticket);
    }

    @GetMapping("/{codigoReferencia}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID codigoReferencia) {
        return ticketService.findByCodigoReferencia(codigoReferencia)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cliente")
    public ResponseEntity<ClienteResponse> createCliente(@Valid @RequestBody ClienteRequest request) {
        log.info("Creating cliente: {}", request.nationalId());
        
        ClienteResponse cliente = clienteService.create(request);
        return ResponseEntity.status(201).body(cliente);
    }

    @GetMapping("/cliente/{nationalId}")
    public ResponseEntity<ClienteResponse> getCliente(@PathVariable String nationalId) {
        return clienteService.findByNationalId(nationalId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}