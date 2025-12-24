package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Feature: Validaciones de Input")
class ValidationIT extends BaseIntegrationTest {

    @Nested
    @DisplayName("Validación de TicketRequest")
    class TicketRequestValidation {

        @Test
        @DisplayName("Cliente inexistente → 404")
        void clienteInexistente_debe404() {
            // When + Then
            given()
                .contentType("application/json")
                .body(createTicketRequest(999999L, "Sucursal Centro", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(404)
                .body("message", containsString("Cliente not found"));
        }

        @Test
        @DisplayName("clienteId null → 400")
        void clienteIdNull_debe400() {
            String request = """
                {
                    "clienteId": null,
                    "branchOffice": "Sucursal Centro",
                    "queueType": "CAJA"
                }
                """;

            given()
                .contentType("application/json")
                .body(request)
            .when()
                .post("/tickets")
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("branchOffice vacío → 400")
        void branchOfficeVacio_debe400() {
            Long clienteId = createTestCliente("12345678", "+56912345678");
            
            String request = String.format("""
                {
                    "clienteId": %d,
                    "branchOffice": "",
                    "queueType": "CAJA"
                }
                """, clienteId);

            given()
                .contentType("application/json")
                .body(request)
            .when()
                .post("/tickets")
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("queueType inválido → 400")
        void queueTypeInvalido_debe400() {
            Long clienteId = createTestCliente("12345678", "+56912345678");
            
            String request = String.format("""
                {
                    "clienteId": %d,
                    "branchOffice": "Sucursal Centro",
                    "queueType": "INVALIDO"
                }
                """, clienteId);

            given()
                .contentType("application/json")
                .body(request)
            .when()
                .post("/tickets")
            .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("Validación de ClienteRequest")
    class ClienteRequestValidation {

        @ParameterizedTest(name = "nationalId={0} → HTTP {1}")
        @CsvSource({
            "1234567, 400",      // 7 dígitos - muy corto
            "12345678, 201",     // 8 dígitos - válido
            "123456789, 201",    // 9 dígitos - válido
            "123456789012, 201", // 12 dígitos - válido
            "1234567890123, 400" // 13 dígitos - muy largo
        })
        @DisplayName("Validar longitud de nationalId")
        void validarLongitud_nationalId(String nationalId, int expectedStatus) {
            given()
                .contentType("application/json")
                .body(createClienteRequest(nationalId, "+56912345678"))
            .when()
                .post("/tickets/cliente")
            .then()
                .statusCode(expectedStatus);
        }

        @Test
        @DisplayName("nationalId con letras → 400")
        void nationalId_conLetras_debeRechazar() {
            given()
                .contentType("application/json")
                .body(createClienteRequest("12345ABC", "+56912345678"))
            .when()
                .post("/tickets/cliente")
            .then()
                .statusCode(400)
                .body("message", containsString("nationalId"));
        }

        @Test
        @DisplayName("nationalId vacío → 400")
        void nationalId_vacio_debeRechazar() {
            String request = """
                {
                    "nationalId": "",
                    "nombre": "Cliente Test",
                    "telefono": "+56912345678"
                }
                """;

            given()
                .contentType("application/json")
                .body(request)
            .when()
                .post("/tickets/cliente")
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("nombre vacío → 400")
        void nombre_vacio_debeRechazar() {
            String request = """
                {
                    "nationalId": "12345678",
                    "nombre": "",
                    "telefono": "+56912345678"
                }
                """;

            given()
                .contentType("application/json")
                .body(request)
            .when()
                .post("/tickets/cliente")
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Cliente duplicado → 409")
        void clienteDuplicado_debe409() {
            // Given - Crear cliente
            given()
                .contentType("application/json")
                .body(createClienteRequest("12345678", "+56912345678"))
            .when()
                .post("/tickets/cliente")
            .then()
                .statusCode(201);

            // When - Intentar crear otro con mismo nationalId
            given()
                .contentType("application/json")
                .body(createClienteRequest("12345678", "+56987654321"))
            .when()
                .post("/tickets/cliente")
            .then()
                .statusCode(409)
                .body("message", containsString("Cliente already exists"));
        }
    }

    @Nested
    @DisplayName("Recursos no encontrados")
    class NotFoundValidation {

        @Test
        @DisplayName("Ticket inexistente → 404")
        void ticket_inexistente_debe404() {
            UUID uuidInexistente = UUID.randomUUID();

            given()
            .when()
                .get("/tickets/" + uuidInexistente)
            .then()
                .statusCode(404)
                .body("message", containsString(uuidInexistente.toString()));
        }

        @Test
        @DisplayName("Cliente inexistente por nationalId → 404")
        void cliente_inexistente_debe404() {
            given()
            .when()
                .get("/tickets/cliente/99999999")
            .then()
                .statusCode(404);
        }
    }
}