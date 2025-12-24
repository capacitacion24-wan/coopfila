package com.example.ticketero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramService - Unit Tests")
class TelegramServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TelegramService telegramService;

    // ============================================================
    // ENVIAR MENSAJE
    // ============================================================
    
    @Nested
    @DisplayName("sendMessage()")
    class EnviarMensaje {

        @Test
        @DisplayName("con respuesta exitosa → debe retornar messageId")
        void sendMessage_conRespuestaExitosa_debeRetornarMessageId() {
            // Given
            ReflectionTestUtils.setField(telegramService, "botToken", "test-token");
            ReflectionTestUtils.setField(telegramService, "apiUrl", "https://api.telegram.org/bot");

            Map<String, Object> responseBody = Map.of(
                "ok", true,
                "result", Map.of("message_id", 12345)
            );
            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

            // When
            String messageId = telegramService.sendMessage("123456789", "Test message");

            // Then
            assertThat(messageId).isEqualTo("12345");
        }

        @Test
        @DisplayName("debe enviar request con datos correctos")
        void sendMessage_debeEnviarRequestConDatosCorrectos() {
            // Given
            ReflectionTestUtils.setField(telegramService, "botToken", "test-token");
            ReflectionTestUtils.setField(telegramService, "apiUrl", "https://api.telegram.org/bot");

            Map<String, Object> responseBody = Map.of(
                "ok", true,
                "result", Map.of("message_id", 12345)
            );
            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

            // When
            telegramService.sendMessage("987654321", "Hello World");

            // Then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            
            verify(restTemplate).postForEntity(urlCaptor.capture(), entityCaptor.capture(), eq(Map.class));

            // Verificar URL
            assertThat(urlCaptor.getValue()).isEqualTo("https://api.telegram.org/bottest-token/sendMessage");

            // Verificar body del request
            HttpEntity<Map<String, Object>> httpEntity = entityCaptor.getValue();
            Map<String, Object> requestBody = httpEntity.getBody();
            
            assertThat(requestBody).containsEntry("chat_id", "987654321");
            assertThat(requestBody).containsEntry("text", "Hello World");
            assertThat(requestBody).containsEntry("parse_mode", "HTML");
        }

        @Test
        @DisplayName("con respuesta de error → debe lanzar RuntimeException")
        void sendMessage_conRespuestaDeError_debeLanzarRuntimeException() {
            // Given
            ReflectionTestUtils.setField(telegramService, "botToken", "test-token");
            ReflectionTestUtils.setField(telegramService, "apiUrl", "https://api.telegram.org/bot");

            ResponseEntity<Map> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

            // When + Then
            assertThatThrownBy(() -> telegramService.sendMessage("123", "Test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send Telegram message");
        }

        @Test
        @DisplayName("con excepción de red → debe lanzar RuntimeException")
        void sendMessage_conExcepcionDeRed_debeLanzarRuntimeException() {
            // Given
            ReflectionTestUtils.setField(telegramService, "botToken", "test-token");
            ReflectionTestUtils.setField(telegramService, "apiUrl", "https://api.telegram.org/bot");

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Network error"));

            // When + Then
            assertThatThrownBy(() -> telegramService.sendMessage("123", "Test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send Telegram message")
                .hasCauseInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("con respuesta sin result → debe lanzar RuntimeException")
        void sendMessage_conRespuestaSinResult_debeLanzarRuntimeException() {
            // Given
            ReflectionTestUtils.setField(telegramService, "botToken", "test-token");
            ReflectionTestUtils.setField(telegramService, "apiUrl", "https://api.telegram.org/bot");

            Map<String, Object> responseBody = Map.of("ok", false);
            ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

            // When + Then
            assertThatThrownBy(() -> telegramService.sendMessage("123", "Test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send Telegram message");
        }
    }

    // ============================================================
    // FORMATEAR MENSAJES
    // ============================================================
    
    @Nested
    @DisplayName("Message Formatting")
    class FormatearMensajes {

        @Test
        @DisplayName("formatTicketCreatedMessage debe incluir todos los datos")
        void formatTicketCreatedMessage_debeIncluirTodosLosDatos() {
            // When
            String message = telegramService.formatTicketCreatedMessage("C05", 3, 15);

            // Then
            assertThat(message).contains("C05");
            assertThat(message).contains("#3");
            assertThat(message).contains("15 minutos");
            assertThat(message).contains("Ticket Creado");
            assertThat(message).contains("Posición en cola");
            assertThat(message).contains("Tiempo estimado");
        }

        @Test
        @DisplayName("formatProximoTurnoMessage debe incluir número de ticket")
        void formatProximoTurnoMessage_debeIncluirNumeroDeTicket() {
            // When
            String message = telegramService.formatProximoTurnoMessage("P02");

            // Then
            assertThat(message).contains("P02");
            assertThat(message).contains("Pronto será tu turno");
            assertThat(message).contains("próximos minutos");
        }

        @Test
        @DisplayName("formatEsTuTurnoMessage debe incluir todos los datos del advisor")
        void formatEsTuTurnoMessage_debeIncluirTodosLosDatosDelAdvisor() {
            // When
            String message = telegramService.formatEsTuTurnoMessage("E01", "María López", 5);

            // Then
            assertThat(message).contains("E01");
            assertThat(message).contains("María López");
            assertThat(message).contains("5");
            assertThat(message).contains("ES TU TURNO");
            assertThat(message).contains("Asesor");
            assertThat(message).contains("Módulo");
        }

        @Test
        @DisplayName("formatTicketCreatedMessage con posición 1 debe mostrar correctamente")
        void formatTicketCreatedMessage_conPosicion1_debeMostrarCorrectamente() {
            // When
            String message = telegramService.formatTicketCreatedMessage("G01", 1, 30);

            // Then
            assertThat(message).contains("G01");
            assertThat(message).contains("#1");
            assertThat(message).contains("30 minutos");
        }

        @Test
        @DisplayName("formatEsTuTurnoMessage con módulo de un dígito debe mostrar correctamente")
        void formatEsTuTurnoMessage_conModuloUnDigito_debeMostrarCorrectamente() {
            // When
            String message = telegramService.formatEsTuTurnoMessage("C99", "Juan García", 1);

            // Then
            assertThat(message).contains("C99");
            assertThat(message).contains("Juan García");
            assertThat(message).contains("1");
        }

        @Test
        @DisplayName("todos los mensajes deben usar formato HTML")
        void todosLosMensajes_debenUsarFormatoHTML() {
            // When
            String ticketCreated = telegramService.formatTicketCreatedMessage("C01", 1, 5);
            String proximoTurno = telegramService.formatProximoTurnoMessage("C01");
            String esTuTurno = telegramService.formatEsTuTurnoMessage("C01", "Asesor", 1);

            // Then - Verificar que contienen tags HTML
            assertThat(ticketCreated).contains("<b>").contains("</b>");
            assertThat(proximoTurno).contains("<b>").contains("</b>");
            assertThat(esTuTurno).contains("<b>").contains("</b>");
        }
    }
}