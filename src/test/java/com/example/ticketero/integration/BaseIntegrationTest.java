package com.example.ticketero.integration;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all integration tests.
 * Uses dockerized PostgreSQL from docker-compose.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // ============================================================
    // SETUP
    // ============================================================

    @BeforeEach
    void setupRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void cleanDatabase() {
        // Limpiar en orden correcto (FK constraints)
        jdbcTemplate.execute("DELETE FROM audit_log");
        jdbcTemplate.execute("DELETE FROM mensaje");
        jdbcTemplate.execute("DELETE FROM ticket");
        jdbcTemplate.execute("DELETE FROM cliente");
        jdbcTemplate.execute("UPDATE advisor SET status = 'AVAILABLE', assigned_tickets_count = 0");
    }

    // ============================================================
    // UTILITIES
    // ============================================================

    protected String createClienteRequest(String nationalId, String telefono) {
        return String.format("""
            {
                "nationalId": "%s",
                "nombre": "Cliente Test",
                "telefono": "%s"
            }
            """, nationalId, telefono);
    }

    protected String createTicketRequest(Long clienteId, String branchOffice, String queueType) {
        return String.format("""
            {
                "clienteId": %d,
                "branchOffice": "%s",
                "queueType": "%s"
            }
            """, clienteId, branchOffice, queueType);
    }

    protected Long createTestCliente(String nationalId, String telefono) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO cliente (national_id, nombre, apellido, telefono, created_at) VALUES (?, ?, ?, ?, NOW()) RETURNING id",
            Long.class, nationalId, "Cliente", "Test", telefono);
    }

    protected int countTicketsInStatus(String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ticket WHERE status = ?",
            Integer.class, status);
    }

    protected int countMensajesInStatus(String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mensaje WHERE estado_envio = ?",
            Integer.class, status);
    }

    protected int countMensajesWithTemplate(String template) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mensaje WHERE plantilla = ?",
            Integer.class, template);
    }

    protected int countAdvisorsInStatus(String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM advisor WHERE status = ?",
            Integer.class, status);
    }

    protected void waitForTicketProcessing(int expectedCompleted, int timeoutSeconds) {
        org.awaitility.Awaitility.await()
            .atMost(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            .until(() -> countTicketsInStatus("COMPLETED") >= expectedCompleted);
    }

    protected void setAdvisorStatus(Long advisorId, String status) {
        jdbcTemplate.update(
            "UPDATE advisor SET status = ? WHERE id = ?",
            status, advisorId);
    }
}