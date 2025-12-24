package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Suite completa de tests E2E para CoopFila.
 * Ejecuta todos los escenarios en orden lógico.
 */
@DisplayName("Suite E2E Completa - CoopFila")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class E2ETestSuite extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. Setup y Validación")
    void setup_debeEstarFuncionando() {
        // Verificar conexión BD
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(1);
        
        // Verificar asesores disponibles
        org.assertj.core.api.Assertions.assertThat(countAdvisorsInStatus("AVAILABLE")).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("2. Creación de Tickets")
    void creacion_debeCrearTickets() {
        Long clienteId = createTestCliente("E2E001", "+56912345678");
        
        io.restassured.RestAssured.given()
            .contentType("application/json")
            .body(createTicketRequest(clienteId, "Sucursal E2E", "CAJA"))
        .when()
            .post("/tickets")
        .then()
            .statusCode(201);
            
        org.assertj.core.api.Assertions.assertThat(countTicketsInStatus("WAITING")).isGreaterThan(0);
    }

    @Test
    @Order(3)
    @DisplayName("3. Procesamiento Automático")
    void procesamiento_debeCompletarTickets() {
        // Esperar que se procesen tickets
        org.awaitility.Awaitility.await()
            .atMost(30, java.util.concurrent.TimeUnit.SECONDS)
            .until(() -> countTicketsInStatus("COMPLETED") >= 1);
            
        org.assertj.core.api.Assertions.assertThat(countTicketsInStatus("COMPLETED")).isGreaterThan(0);
    }

    @Test
    @Order(4)
    @DisplayName("4. Validaciones de Input")
    void validaciones_debenRechazarInputInvalido() {
        // Cliente inexistente
        io.restassured.RestAssured.given()
            .contentType("application/json")
            .body(createTicketRequest(999999L, "Sucursal Test", "CAJA"))
        .when()
            .post("/tickets")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(5)
    @DisplayName("5. Dashboard Admin")
    void dashboard_debeRetornarEstadisticas() {
        io.restassured.RestAssured.given()
        .when()
            .get("/admin/dashboard")
        .then()
            .statusCode(200);
    }
}