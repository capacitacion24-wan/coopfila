package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import com.example.ticketero.service.AdvisorService;
import com.example.ticketero.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProcessorScheduler {

    private final TicketRepository ticketRepository;
    private final AdvisorService advisorService;
    private final AuditService auditService;

    @Scheduled(fixedRate = 5000) // Cada 5 segundos
    @Transactional
    public void processQueue() {
        // Buscar tickets en espera
        List<Ticket> waitingTickets = ticketRepository.findByStatusWithCliente(TicketStatus.EN_ESPERA);
        
        if (!waitingTickets.isEmpty()) {
            log.debug("Processing {} waiting tickets", waitingTickets.size());
            
            for (Ticket ticket : waitingTickets) {
                // Marcar como próximo si está en posición <= 3
                if (ticket.getPositionInQueue() <= 3 && ticket.getStatus() == TicketStatus.EN_ESPERA) {
                    ticket.setStatus(TicketStatus.PROXIMO);
                    auditService.logTicketStatusChange(ticket.getId(), "EN_ESPERA", "PROXIMO");
                    log.info("Ticket {} marked as PROXIMO", ticket.getNumero());
                }
                
                // Asignar a asesor disponible si es el primero en cola
                if (ticket.getPositionInQueue() == 1 && ticket.getStatus() == TicketStatus.PROXIMO) {
                    Optional<Advisor> availableAdvisor = advisorService.findBestAvailableAdvisor();
                    
                    if (availableAdvisor.isPresent()) {
                        Advisor advisor = availableAdvisor.get();
                        ticket.setAssignedAdvisor(advisor);
                        ticket.setAssignedModuleNumber(advisor.getModuleNumber());
                        ticket.setStatus(TicketStatus.ATENDIENDO);
                        
                        advisorService.assignTicket(advisor.getId());
                        auditService.logTicketStatusChange(ticket.getId(), "PROXIMO", "ATENDIENDO");
                        
                        log.info("Ticket {} assigned to advisor {} at module {}", 
                            ticket.getNumero(), advisor.getName(), advisor.getModuleNumber());
                    }
                }
            }
        }
        
        // Actualizar posiciones en cola
        updateQueuePositions();
    }

    private void updateQueuePositions() {
        List<Ticket> activeTickets = ticketRepository.findByStatusWithCliente(TicketStatus.EN_ESPERA);
        
        for (int i = 0; i < activeTickets.size(); i++) {
            Ticket ticket = activeTickets.get(i);
            int newPosition = i + 1;
            
            if (ticket.getPositionInQueue() != newPosition) {
                ticket.setPositionInQueue(newPosition);
                ticket.setEstimatedWaitMinutes(newPosition * ticket.getQueueType().getAvgTimeMinutes());
            }
        }
    }
}