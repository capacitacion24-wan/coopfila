package com.example.ticketero.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Feature: Creación de Tickets")
class TicketCreationIT extends BaseIntegrationTest {

    @Nested
    @DisplayName("Escenarios Happy Path (P0)")
    class HappyPath {

        @Test
        @DisplayName("Crear ticket con cliente existente → 201 + status WAITING")
        void crearTicket_clienteExistente_debeCrear() {
            // Given - Cliente existente y asesores disponibles
            Long clienteId = createTestCliente("12345678", "+56912345678");
            assertThat(countAdvisorsInStatus("AVAILABLE")).isGreaterThan(0);

            // When
            Response response = given()
                .contentType("application/json")
                .body(createTicketRequest(clienteId, "Sucursal Centro", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201)
                .body("numero", notNullValue())
                .body("status", equalTo("WAITING"))
                .body("queueType", equalTo("CAJA"))
                .body("positionInQueue", greaterThan(0))
                .body("estimatedWaitMinutes", greaterThanOrEqualTo(0))
                .body("codigoReferencia", notNullValue())
                .extract().response();

            // Then - Verificar BD
            assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(1);
            
            // Verificar mensaje programado
            int mensajesPendientes = countMensajesInStatus("PENDING");
            assertThat(mensajesPendientes).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Calcular posición correcta con tickets existentes")
        void crearTicket_conTicketsExistentes_debePosicionCorrecta() {
            // Given - Crear 3 tickets previos
            Long clienteId = createTestCliente("10000001", "+56912345678");
            
            for (int i = 1; i <= 3; i++) {
                given()
                    .contentType("application/json")
                    .body(createTicketRequest(clienteId, "Sucursal Centro", "CAJA"))
                .when()
                    .post("/tickets")
                .then()
                    .statusCode(201);
            }

            // When - Crear ticket #4
            Response response = given()
                .contentType("application/json")
                .body(createTicketRequest(clienteId, "Sucursal Centro", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201)
                .extract().response();

            // Then
            int posicion = response.jsonPath().getInt("positionInQueue");
            int tiempoEstimado = response.jsonPath().getInt("estimatedWaitMinutes");

            assertThat(posicion).isEqualTo(4);
            // Tiempo = (posición - 1) × avgTime(5) = 3 × 5 = 15
            assertThat(tiempoEstimado).isEqualTo(15);
        }

        @Test
        @DisplayName("Crear tickets para diferentes colas → posiciones independientes")
        void crearTickets_diferentesColas_posicionesIndependientes() {
            // Given
            Long clienteId = createTestCliente("20000001", "+56912345678");
            String[] colas = {"CAJA", "PERSONAL", "EMPRESAS", "GERENCIA"};
            String[] prefijos = {"C", "P", "E", "G"};

            // When + Then - Crear un ticket por cada cola
            for (int i = 0; i < colas.length; i++) {
                Response response = given()
                    .contentType("application/json")
                    .body(createTicketRequest(clienteId, "Sucursal Centro", colas[i]))
                .when()
                    .post("/tickets")
                .then()
                    .statusCode(201)
                    .extract().response();

                // Cada cola empieza en posición 1
                assertThat(response.jsonPath().getInt("positionInQueue")).isEqualTo(1);
                assertThat(response.jsonPath().getString("numero")).startsWith(prefijos[i]);
            }

            // Verificar mensajes programados
            assertThat(countMensajesInStatus("PENDING")).isGreaterThanOrEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Escenarios Edge Case (P1)")
    class EdgeCases {

        @Test
        @DisplayName("Número de ticket tiene formato correcto")
        void crearTicket_debeGenerarNumeroConFormato() {
            // Given
            Long clienteId = createTestCliente("11111111", "+56912345678");

            // When
            Response response = given()
                .contentType("application/json")
                .body(createTicketRequest(clienteId, "Sucursal Centro", "PERSONAL"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201)
                .extract().response();

            // Then
            String numero = response.jsonPath().getString("numero");
            assertThat(numero).matches("P\\d{3}"); // P seguido de 3 dígitos
        }

        @Test
        @DisplayName("Consultar ticket por código de referencia")
        void consultarTicket_porCodigo_debeRetornarDatos() {
            // Given - Crear ticket
            Long clienteId = createTestCliente("22222222", "+56912345678");
            
            Response createResponse = given()
                .contentType("application/json")
                .body(createTicketRequest(clienteId, "Sucursal Centro", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201)
                .extract().response();

            String codigoReferencia = createResponse.jsonPath().getString("codigoReferencia");

            // When + Then
            given()
            .when()
                .get("/tickets/" + codigoReferencia)
            .then()
                .statusCode(200)
                .body("codigoReferencia", equalTo(codigoReferencia))
                .body("status", equalTo("WAITING"))
                .body("positionInQueue", notNullValue())
                .body("estimatedWaitMinutes", notNullValue());
        }

        @Test
        @DisplayName("Crear cliente y ticket en flujo completo")
        void crearClienteYTicket_flujoCompleto() {
            // When - Crear cliente
            Response clienteResponse = given()
                .contentType("application/json")
                .body(createClienteRequest("33333333", "+56987654321"))
            .when()
                .post("/tickets/cliente")
            .then()
                .statusCode(201)
                .extract().response();

            Long clienteId = clienteResponse.jsonPath().getLong("id");

            // When - Crear ticket para ese cliente
            given()
                .contentType("application/json")
                .body(createTicketRequest(clienteId, "Sucursal Norte", "EMPRESAS"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201)
                .body("clienteNombre", equalTo("Cliente Test"))
                .body("queueType", equalTo("EMPRESAS"));
        }
    }
}