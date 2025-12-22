package com.example.ticketero.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

    private final RestTemplate restTemplate;

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.api-url:https://api.telegram.org/bot}")
    private String apiUrl;

    public String sendMessage(String chatId, String text) {
        try {
            String url = apiUrl + botToken + "/sendMessage";
            
            Map<String, Object> request = Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
                String messageId = result.get("message_id").toString();
                log.info("Message sent successfully to chat {}: messageId {}", chatId, messageId);
                return messageId;
            } else {
                log.error("Failed to send message to Telegram: {}", response.getBody());
                throw new RuntimeException("Telegram API error");
            }
            
        } catch (Exception e) {
            log.error("Error sending message to Telegram: {}", e.getMessage());
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }

    public String formatTicketCreatedMessage(String ticketNumber, int position, int estimatedMinutes) {
        return String.format("""
            ✅ <b>Ticket Creado</b>
            
            🎫 Número: <b>%s</b>
            📍 Posición en cola: <b>#%d</b>
            ⏱️ Tiempo estimado: <b>%d minutos</b>
            
            Te notificaremos cuando sea tu turno.
            """, ticketNumber, position, estimatedMinutes);
    }

    public String formatProximoTurnoMessage(String ticketNumber) {
        return String.format("""
            ⏰ <b>Pronto será tu turno</b>
            
            🎫 Ticket: <b>%s</b>
            
            Prepárate, serás atendido en los próximos minutos.
            """, ticketNumber);
    }

    public String formatEsTuTurnoMessage(String ticketNumber, String advisorName, int moduleNumber) {
        return String.format("""
            🔔 <b>¡ES TU TURNO!</b>
            
            🎫 Ticket: <b>%s</b>
            👤 Asesor: <b>%s</b>
            🏢 Módulo: <b>%d</b>
            
            Dirígete al módulo indicado.
            """, ticketNumber, advisorName, moduleNumber);
    }
}