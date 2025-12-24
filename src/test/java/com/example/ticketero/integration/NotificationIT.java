package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Feature: Notificaciones Telegram")
class NotificationIT extends BaseIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Usar configuración real de Telegram de test-data
        registry.add("telegram.bot.token", () -> "8591640924:AAG7t3qQ52aOvzEC2XtNh9BhHRPxdqe4VVg");
        registry.add("telegram.bot.chat-id", () -> "1634964503");
        registry.add("telegram.api.url", () -> "https://api.telegram.org");
    }

    @Test
    @DisplayName("Notificación #1 - Confirmación al crear ticket")
    void crearTicket_debeEnviarNotificacion() {
        // Given - Cliente con teléfono configurado para Telegram
        Long clienteId = createTestCliente("12345678", "1634964503");

        // When - Crear ticket
        given()
            .contentType("application/json")
            .body(createTicketRequest(clienteId, "Sucursal Centro", "CAJA"))
        .when()
            .post("/tickets")
        .then()
            .statusCode(201)
            .body("numero", containsString("C"));

        // Then - Verificar que se programó mensaje de confirmación
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countMensajesInStatus("PENDIENTE") >= 1);
    }

    @Test
    @DisplayName("Notificación #2 - Próximo turno cuando posición ≤ 3")
    void posicionProxima_debeNotificarProximoTurno() {
        // Given - Crear 4 tickets para generar cola
        for (int i = 1; i <= 4; i++) {
            Long clienteId = createTestCliente("1111111" + i, "1634964503");
            given()
                .contentType("application/json")
                .body(createTicketRequest(clienteId, "Sucursal Centro", "CAJA"))
            .when()
                .post("/tickets");
        }

        // When - Esperar que se procesen algunos tickets
        await()
            .atMost(60, TimeUnit.SECONDS)
            .until(() -> countTicketsInStatus("COMPLETED") >= 1);

        // Then - Debería haberse programado notificación de próximo turno
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countMensajesWithTemplate("PROXIMO_TURNO") >= 1);
    }

    @Test
    @DisplayName("Notificación #3 - Es tu turno (incluye asesor y módulo)")
    void procesarTicket_debeNotificarTurnoActivo() {
        // Given - Cliente con teléfono
        Long clienteId = createTestCliente("99999999", "1634964503");

        // When - Crear ticket
        given()
            .contentType("application/json")
            .body(createTicketRequest(clienteId, "Sucursal Norte", "CAJA"))
        .when()
            .post("/tickets")
        .then()
            .statusCode(201);

        // Then - Esperar notificación de turno activo
        await()
            .atMost(30, TimeUnit.SECONDS)
            .until(() -> countMensajesWithTemplate("ES_TU_TURNO") >= 1);
    }

    @Test
    @DisplayName("Cliente sin teléfono → no se programan notificaciones")
    void clienteSinTelefono_noSeNotifica() {
        // Given - Cliente sin teléfono
        Long clienteId = createTestCliente("10101010", null);
        
        // When - Crear ticket
        given()
            .contentType("application/json")
            .body(createTicketRequest(clienteId, "Sucursal Centro", "CAJA"))
        .when()
            .post("/tickets")
        .then()
            .statusCode(201);

        // Then - El ticket debe procesarse normalmente
        await()
            .atMost(30, TimeUnit.SECONDS)
            .until(() -> countTicketsInStatus("COMPLETED") >= 1);

        // Verificar que NO se programaron mensajes
        int mensajesProgramados = countMensajesInStatus("PENDIENTE");
        assert mensajesProgramados == 0 : "No deberían programarse mensajes para cliente sin teléfono";
    }

    @Test
    @DisplayName("Endpoint de prueba Telegram funciona")
    void testTelegramEndpoint_debeEnviarMensaje() {
        // When - Enviar mensaje de prueba
        given()
            .queryParam("chatId", "1634964503")
            .queryParam("message", "🧪 Test E2E desde NotificationIT")
        .when()
            .post("/api/test/telegram")
        .then()
            .statusCode(200);

        // Then - El mensaje debería enviarse correctamente
        // (verificación manual en Telegram)
    }
}