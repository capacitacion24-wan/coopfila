package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.service.AuditService;
import com.example.ticketero.service.MensajeService;
import com.example.ticketero.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageScheduler {

    private final MensajeService mensajeService;
    private final AuditService auditService;
    private final TelegramService telegramService;

    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    public void processPendingMessages() {
        List<Mensaje> pendingMessages = mensajeService.findPendingMessages();
        
        if (!pendingMessages.isEmpty()) {
            log.info("Processing {} pending messages", pendingMessages.size());
            
            for (Mensaje mensaje : pendingMessages) {
                try {
                    // Simular envío a Telegram
                    boolean sent = sendToTelegram(mensaje);
                    
                    if (sent) {
                        String telegramId = "msg_" + System.currentTimeMillis();
                        mensajeService.markAsSent(mensaje.getId(), telegramId);
                        auditService.logMessageSent(mensaje.getId(), mensaje.getPlantilla().name());
                    } else {
                        mensajeService.markAsFailed(mensaje.getId());
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing message {}: {}", mensaje.getId(), e.getMessage());
                    mensajeService.markAsFailed(mensaje.getId());
                }
            }
        }
    }

    private boolean sendToTelegram(Mensaje mensaje) {
        try {
            String chatId = mensaje.getTicket().getCliente().getTelefono();
            if (chatId == null || chatId.isEmpty()) {
                log.warn("No phone number for ticket {}", mensaje.getTicket().getNumero());
                return false;
            }

            String messageText = formatMessage(mensaje);
            telegramService.sendMessage(chatId, messageText);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send Telegram message: {}", e.getMessage());
            return false;
        }
    }

    private String formatMessage(Mensaje mensaje) {
        return switch (mensaje.getPlantilla()) {
            case TOTEM_TICKET_CREADO -> telegramService.formatTicketCreatedMessage(
                mensaje.getTicket().getNumero(),
                mensaje.getTicket().getPositionInQueue(),
                mensaje.getTicket().getEstimatedWaitMinutes()
            );
            case TOTEM_PROXIMO_TURNO -> telegramService.formatProximoTurnoMessage(
                mensaje.getTicket().getNumero()
            );
            case TOTEM_ES_TU_TURNO -> telegramService.formatEsTuTurnoMessage(
                mensaje.getTicket().getNumero(),
                mensaje.getTicket().getAssignedAdvisor() != null ? 
                    mensaje.getTicket().getAssignedAdvisor().getName() : "Asesor",
                mensaje.getTicket().getAssignedModuleNumber() != null ? 
                    mensaje.getTicket().getAssignedModuleNumber() : 1
            );
        };
    }
}