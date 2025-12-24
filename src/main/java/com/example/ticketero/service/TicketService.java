package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.Cliente;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.ClienteRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final MensajeService mensajeService;

    @Transactional
    public TicketResponse create(TicketRequest request) {
        log.info("Creating ticket for cliente: {}", request.clienteId());

        Cliente cliente = clienteRepository.findById(request.clienteId())
            .orElseThrow(() -> new RuntimeException("Cliente not found"));

        // Calcular posición en cola
        long queuePosition = ticketRepository.countByQueueTypeAndStatus(
            request.queueType(), TicketStatus.EN_ESPERA) + 1;

        // Generar número de ticket
        String numero = generateTicketNumber(request.queueType(), queuePosition);

        Ticket ticket = Ticket.builder()
            .cliente(cliente)
            .branchOffice(request.branchOffice())
            .queueType(request.queueType())
            .status(TicketStatus.EN_ESPERA)
            .positionInQueue((int) queuePosition)
            .estimatedWaitMinutes((int) (queuePosition * request.queueType().getAvgTimeMinutes()))
            .numero(numero)
            .build();

        Ticket saved = ticketRepository.save(ticket);
        
        // Programar mensajes
        mensajeService.scheduleMessages(saved);
        
        return toResponse(saved);
    }

    public Optional<TicketResponse> findByCodigoReferencia(UUID codigoReferencia) {
        return ticketRepository.findByCodigoReferencia(codigoReferencia)
            .map(this::toResponse);
    }

    public List<TicketResponse> findActiveTickets() {
        return ticketRepository.findByStatusWithCliente(TicketStatus.EN_ESPERA)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private String generateTicketNumber(com.example.ticketero.model.enums.QueueType queueType, long position) {
        return queueType.getPrefix() + String.format("%03d", position);
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getId(),
            ticket.getCodigoReferencia(),
            ticket.getNumero(),
            ticket.getCliente().getId(),
            ticket.getCliente().getNombre() + " " + ticket.getCliente().getApellido(),
            ticket.getBranchOffice(),
            ticket.getQueueType().name(),
            ticket.getStatus().name(),
            ticket.getPositionInQueue(),
            ticket.getEstimatedWaitMinutes(),
            ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getId() : null,
            ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getName() : null,
            ticket.getAssignedModuleNumber(),
            ticket.getCreatedAt()
        );
    }
}