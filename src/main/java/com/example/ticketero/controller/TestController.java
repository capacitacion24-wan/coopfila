package com.example.ticketero.controller;

import com.example.ticketero.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final TelegramService telegramService;

    @PostMapping("/telegram")
    public ResponseEntity<Map<String, String>> testTelegram(
        @RequestParam String chatId,
        @RequestParam(defaultValue = "🤖 Test message from Ticketero API") String message
    ) {
        try {
            log.info("Testing Telegram with chatId: {}", chatId);
            String messageId = telegramService.sendMessage(chatId, message);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "messageId", messageId,
                "chatId", chatId
            ));
        } catch (Exception e) {
            log.error("Telegram test failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/telegram/formats")
    public ResponseEntity<Map<String, String>> getMessageFormats() {
        Map<String, String> formats = Map.of(
            "ticket_created", telegramService.formatTicketCreatedMessage("C01", 5, 25),
            "proximo_turno", telegramService.formatProximoTurnoMessage("C01"),
            "es_tu_turno", telegramService.formatEsTuTurnoMessage("C01", "María González", 3)
        );
        
        return ResponseEntity.ok(formats);
    }
}