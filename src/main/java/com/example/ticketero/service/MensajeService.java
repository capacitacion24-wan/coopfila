package com.example.ticketero.service;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageStatus;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MensajeService {

    private final MensajeRepository mensajeRepository;

    @Transactional
    public void scheduleMessages(Ticket ticket) {
        log.info("Scheduling messages for ticket: {}", ticket.getNumero());

        // Mensaje inmediato: ticket creado
        createMessage(ticket, MessageTemplate.TOTEM_TICKET_CREADO, LocalDateTime.now());

        // Mensaje cuando esté próximo (estimado)
        LocalDateTime proximoTime = LocalDateTime.now()
            .plusMinutes(Math.max(0, ticket.getEstimatedWaitMinutes() - 5));
        createMessage(ticket, MessageTemplate.TOTEM_PROXIMO_TURNO, proximoTime);

        // Mensaje cuando sea su turno (estimado)
        LocalDateTime turnoTime = LocalDateTime.now()
            .plusMinutes(ticket.getEstimatedWaitMinutes());
        createMessage(ticket, MessageTemplate.TOTEM_ES_TU_TURNO, turnoTime);
    }

    public List<Mensaje> findPendingMessages() {
        return mensajeRepository.findPendingMessages(
            MessageStatus.PENDIENTE, 
            LocalDateTime.now()
        );
    }

    @Transactional
    public void markAsSent(Long mensajeId, String telegramMessageId) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
            .orElseThrow(() -> new RuntimeException("Mensaje not found"));
        
        mensaje.setEstadoEnvio(MessageStatus.ENVIADO);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setTelegramMessageId(telegramMessageId);
        
        log.info("Message marked as sent: {}", mensajeId);
    }

    @Transactional
    public void markAsFailed(Long mensajeId) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
            .orElseThrow(() -> new RuntimeException("Mensaje not found"));
        
        mensaje.setIntentos(mensaje.getIntentos() + 1);
        
        if (mensaje.getIntentos() >= 3) {
            mensaje.setEstadoEnvio(MessageStatus.FALLIDO);
            log.warn("Message failed after 3 attempts: {}", mensajeId);
        } else {
            // Reprogramar con backoff exponencial
            LocalDateTime nextAttempt = LocalDateTime.now()
                .plusSeconds(30L * mensaje.getIntentos());
            mensaje.setFechaProgramada(nextAttempt);
            log.info("Message rescheduled for attempt {}: {}", mensaje.getIntentos() + 1, mensajeId);
        }
    }

    private void createMessage(Ticket ticket, MessageTemplate template, LocalDateTime scheduledTime) {
        Mensaje mensaje = Mensaje.builder()
            .ticket(ticket)
            .plantilla(template)
            .estadoEnvio(MessageStatus.PENDIENTE)
            .fechaProgramada(scheduledTime)
            .intentos(0)
            .build();

        mensajeRepository.save(mensaje);
    }
}