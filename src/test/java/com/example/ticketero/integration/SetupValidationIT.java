package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Setup Dockerizado - Validación")
class SetupValidationIT extends BaseIntegrationTest {

    @Test
    @DisplayName("PostgreSQL dockerizado debe estar funcionando")
    void postgresDockerizado_debeEstarFuncionando() {
        // Given + When + Then - Verificar conexión a BD
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("RestAssured debe estar configurado correctamente")
    void restAssured_debeEstarConfigurado() {
        // Given + When + Then
        assertThat(port).isGreaterThan(0);
    }

    @Test
    @DisplayName("Utilidades de BD deben funcionar")
    void utilidadesBD_debenFuncionar() {
        // Given + When
        Long clienteId = createTestCliente("12345678", "+56912345678");
        
        // Then
        assertThat(clienteId).isNotNull();
        assertThat(clienteId).isGreaterThan(0);
        
        // Verificar que se creó en BD
        String nationalId = jdbcTemplate.queryForObject(
            "SELECT national_id FROM cliente WHERE id = ?",
            String.class, clienteId);
        assertThat(nationalId).isEqualTo("12345678");
    }
}