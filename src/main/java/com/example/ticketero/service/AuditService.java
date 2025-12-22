package com.example.ticketero.service;

import com.example.ticketero.model.entity.AuditLog;
import com.example.ticketero.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logEvent(String tipoEvento, String actor, String entityType, 
                        String entityId, Map<String, Object> cambiosEstado) {
        
        AuditLog auditLog = AuditLog.builder()
            .tipoEvento(tipoEvento)
            .actor(actor)
            .entityType(entityType)
            .entityId(entityId)
            .cambiosEstado(cambiosEstado)
            .timestamp(LocalDateTime.now())
            .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit event logged: {} for {}", tipoEvento, entityId);
    }

    @Transactional
    public void logTicketCreated(Long ticketId, String clienteNationalId) {
        logEvent(
            "TICKET_CREADO",
            "SYSTEM",
            "Ticket",
            ticketId.toString(),
            Map.of("cliente", clienteNationalId, "status", "EN_ESPERA")
        );
    }

    @Transactional
    public void logTicketStatusChange(Long ticketId, String oldStatus, String newStatus) {
        logEvent(
            "TICKET_STATUS_CHANGED",
            "SYSTEM",
            "Ticket",
            ticketId.toString(),
            Map.of("oldStatus", oldStatus, "newStatus", newStatus)
        );
    }

    @Transactional
    public void logMessageSent(Long mensajeId, String template) {
        logEvent(
            "MESSAGE_SENT",
            "TELEGRAM_BOT",
            "Mensaje",
            mensajeId.toString(),
            Map.of("template", template, "status", "ENVIADO")
        );
    }
}