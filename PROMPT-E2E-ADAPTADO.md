# PROMPT E2E ADAPTADO - COOPFILA

## Contexto
Eres un QA Engineer Senior experto en testing E2E. Tu tarea es crear pruebas funcionales de integración con TestContainers + RestAssured para el sistema CoopFila, validando flujos completos de negocio.

## Características del proyecto:
- API REST con Spring Boot 3.2, Java 21
- PostgreSQL 15 (NO RabbitMQ)
- Telegram Bot API (mockeado con WireMock)
- 4 colas de atención (CAJA, PERSONAL, EMPRESAS, GERENCIA)
- Notificaciones automáticas vía Telegram
- Procesamiento directo (sin colas de mensajería)

**IMPORTANTE:** Después de completar CADA paso, debes DETENERTE y solicitar una revisión exhaustiva antes de continuar.

## Documentos de Entrada
Lee estos archivos del proyecto:
- `src/main/java/com/example/ticketero/controller/` - Endpoints a testear
- `src/main/java/com/example/ticketero/model/dto/` - Request/Response DTOs
- `docker-compose.yml` - Servicios de infraestructura
- `docs/ARQUITECTURA.md` - Flujos de negocio

## Metodología de Trabajo
**Principio:** "Diseñar → Implementar → Ejecutar → Confirmar → Continuar"

Después de CADA paso:
- ✅ Diseña los escenarios de prueba (Gherkin)
- ✅ Implementa los tests con TestContainers
- ✅ Ejecuta `mvn test -Dtest=NombreIT`
- ⏸️ **DETENTE** y solicita revisión
- ✅ Espera confirmación antes de continuar

### Formato de Solicitud de Revisión:
```
✅ PASO X COMPLETADO

Escenarios implementados:
- [Escenario 1]
- [Escenario 2]
- ...

Validaciones:
- HTTP: ✅
- Base de datos: ✅
- Telegram: ✅ (mock)

🔍 SOLICITO REVISIÓN EXHAUSTIVA:
1. ¿Los escenarios cubren el flujo de negocio?
2. ¿Las validaciones son suficientes?
3. ¿Puedo continuar con el siguiente paso?

⏸️ ESPERANDO CONFIRMACIÓN...
```

## Stack de Testing E2E

| Componente | Versión | Propósito |
|------------|---------|-----------|
| JUnit 5 (Jupiter) | 5.10+ | Framework de testing |
| TestContainers | 1.19+ | PostgreSQL real |
| RestAssured | 5.4+ | Testing de APIs REST |
| WireMock | 2.35+ | Mock de Telegram API |
| Awaitility | 4.2+ | Esperas asíncronas |

**Diferencia con Unit Tests:**
- ✅ `@SpringBootTest` con contexto completo
- ✅ Base de datos real (TestContainers)
- ✅ Telegram mockeado (WireMock)
- ❌ NO RabbitMQ (procesamiento directo)

## Tu Tarea: 7 Pasos

1. **PASO 1:** Setup TestContainers + Base de Tests
2. **PASO 2:** Feature: Creación de Tickets (6 escenarios)
3. **PASO 3:** Feature: Procesamiento de Tickets (5 escenarios)
4. **PASO 4:** Feature: Notificaciones Telegram (4 escenarios)
5. **PASO 5:** Feature: Validaciones de Input (5 escenarios)
6. **PASO 6:** Feature: Dashboard Admin (4 escenarios)
7. **PASO 7:** Ejecución Final y Reporte

**Total:** ~24 escenarios E2E | Cobertura de flujos: 100%

## Estructura de Archivos a Crear/Actualizar

```
src/test/java/com/example/ticketero/
├── integration/
│   ├── BaseIntegrationTest.java          ← ACTUALIZAR
│   ├── TicketCreationIT.java             ← CREAR
│   ├── TicketProcessingIT.java           ← CREAR
│   ├── NotificationIT.java               ← CREAR
│   ├── ValidationIT.java                 ← CREAR
│   └── AdminDashboardIT.java             ← CREAR
└── config/
    └── WireMockConfig.java               ← YA EXISTE
```

---

## PASO 1: Setup TestContainers + Base de Tests

**Objetivo:** Actualizar infraestructura de testing E2E para CoopFila.

### 1.1 Actualizar BaseIntegrationTest.java

```java
package com.example.ticketero.integration;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 * Provides TestContainers setup and common utilities.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // ============================================================
    // TESTCONTAINERS
    // ============================================================

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("ticketero_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Telegram Mock (WireMock)
        registry.add("telegram.api-url", () -> "http://localhost:8089/bot");
        registry.add("telegram.bot-token", () -> "test-token");
        registry.add("telegram.chat-id", () -> "123456789");
    }

    // ============================================================
    // SETUP
    // ============================================================

    @BeforeAll
    static void setupContainers() {
        postgres.start();
    }

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
        jdbcTemplate.execute("UPDATE advisor SET status = 'AVAILABLE', total_tickets_served = 0");
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
            "INSERT INTO cliente (national_id, nombre, telefono) VALUES (?, ?, ?) RETURNING id",
            Long.class, nationalId, "Cliente Test", telefono);
    }

    protected int countTicketsInStatus(String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ticket WHERE status = ?",
            Integer.class, status);
    }

    protected int countMensajesInStatus(String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mensaje WHERE status = ?",
            Integer.class, status);
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
```

### Validaciones:
```bash
mvn test -Dtest=BaseIntegrationTest
# Containers start correctly
```

---

## PASO 2: Feature - Creación de Tickets

**Objetivo:** Validar flujo completo de creación de tickets.

### Escenarios Gherkin
```gherkin
Feature: Creación de Tickets
  Como usuario del sistema
  Quiero crear un ticket de atención
  Para ser atendido en la sucursal

  @P0 @HappyPath
  Scenario: Crear ticket con cliente existente
    Given existe un cliente registrado
    And hay asesores disponibles para cola "CAJA"
    When envío POST /api/tickets con clienteId válido
    Then recibo respuesta 201 Created
    And el ticket tiene status "WAITING"
    And el ticket tiene posición calculada en cola

  @P0 @HappyPath
  Scenario: Calcular posición correcta en cola con tickets existentes
    Given existen 3 tickets en estado WAITING para cola "CAJA"
    When creo un nuevo ticket para cola "CAJA"
    Then el nuevo ticket tiene posición 4
    And el tiempo estimado es 15 minutos (3 × 5 min promedio)

  @P0 @HappyPath
  Scenario: Crear tickets para diferentes colas
    Given hay asesores disponibles para todas las colas
    When creo tickets para CAJA, PERSONAL, EMPRESAS y GERENCIA
    Then cada ticket tiene su posición independiente por cola
    And se programan mensajes de notificación

  @P1 @EdgeCase
  Scenario: Crear ticket genera número único con prefijo de cola
    When creo un ticket para cola "PERSONAL"
    Then el número de ticket empieza con "P"
    And tiene formato PXXX (3 dígitos)

  @P1 @EdgeCase
  Scenario: Consultar ticket por código de referencia
    Given existe un ticket creado con código de referencia conocido
    When consulto GET /api/tickets/{codigoReferencia}
    Then recibo los datos completos del ticket
    And incluye posición actual y tiempo estimado

  @P1 @EdgeCase
  Scenario: Crear cliente y ticket en flujo completo
    When creo un cliente nuevo con nationalId único
    And creo un ticket para ese cliente
    Then ambos se crean correctamente
    And el ticket referencia al cliente correcto
```

### TicketCreationIT.java
```java
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
```

---

## PASO 3: Feature - Procesamiento de Tickets

**Objetivo:** Validar flujo completo de procesamiento automático.

### Escenarios Gherkin
```gherkin
Feature: Procesamiento de Tickets
  Como sistema
  Quiero procesar tickets automáticamente
  Para asignar asesores y completar atenciones

  @P0 @HappyPath
  Scenario: Procesar ticket completo (WAITING → COMPLETED)
    Given existe un ticket en estado WAITING
    And hay un asesor disponible
    When el sistema procesa el ticket automáticamente
    Then el ticket pasa por estados CALLED → IN_PROGRESS → COMPLETED
    And el asesor queda AVAILABLE al finalizar
    And se incrementa el contador de tickets servidos

  @P0 @HappyPath
  Scenario: Múltiples tickets se procesan en orden FIFO
    Given existen 3 tickets en cola CAJA en orden de creación
    When el sistema procesa los tickets
    Then se completan en el mismo orden de creación

  @P1 @EdgeCase
  Scenario: Sin asesores disponibles → ticket permanece en cola
    Given todos los asesores están BUSY
    When se crea un nuevo ticket
    Then el ticket permanece en WAITING
    And no se asigna ningún asesor

  @P1 @EdgeCase
  Scenario: Asesor en BREAK no recibe tickets
    Given hay 2 asesores: uno AVAILABLE y uno en BREAK
    When se procesa un ticket
    Then solo el asesor AVAILABLE es asignado

  @P1 @EdgeCase
  Scenario: Ticket ya completado no se reprocesa
    Given existe un ticket en estado COMPLETED
    When el sistema intenta procesarlo nuevamente
    Then el ticket mantiene su estado COMPLETED
    And no se modifica el asesor
```

---

## PASO 4: Feature - Notificaciones Telegram

**Objetivo:** Validar envío de notificaciones automáticas.

### Escenarios Gherkin
```gherkin
Feature: Notificaciones Telegram
  Como usuario
  Quiero recibir notificaciones de mi turno
  Para saber cuándo ser atendido

  @P0 @HappyPath
  Scenario: Notificación #1 - Confirmación al crear ticket
    Given creo un ticket con teléfono válido
    Then se programa notificación de confirmación
    And el mensaje incluye número de ticket y posición

  @P0 @HappyPath
  Scenario: Notificación #2 - Próximo turno (posición ≤ 3)
    Given mi ticket está en posición 4
    When tickets anteriores se completan
    And mi posición pasa a 2
    Then se programa notificación "Tu turno está próximo"

  @P0 @HappyPath
  Scenario: Notificación #3 - Es tu turno
    Given mi ticket está siendo procesado
    When el asesor me llama
    Then se programa notificación con asesor y módulo asignado

  @P1 @EdgeCase
  Scenario: Cliente sin teléfono → no se programan notificaciones
    Given creo un ticket para cliente sin teléfono
    When el ticket se procesa normalmente
    Then no se programan mensajes de notificación
    And el flujo continúa sin errores
```

---

## PASO 5: Feature - Validaciones de Input

**Objetivo:** Validar reglas de negocio y restricciones de entrada.

### Escenarios Gherkin
```gherkin
Feature: Validaciones de Input
  Como sistema
  Quiero validar los datos de entrada
  Para mantener integridad de datos

  @P1 @Validation
  Scenario Outline: Validar campos requeridos en TicketRequest
    When envío POST /api/tickets con <campo> <valor>
    Then recibo respuesta <codigo>
    
    Examples:
      | campo         | valor      | codigo |
      | clienteId     | null       | 400    |
      | clienteId     | 999999     | 404    | # Cliente no existe
      | branchOffice  | ""         | 400    |
      | branchOffice  | null       | 400    |
      | queueType     | null       | 400    |
      | queueType     | "INVALID"  | 400    |

  @P1 @Validation
  Scenario: Cliente inexistente → 404
    When envío POST /api/tickets con clienteId que no existe
    Then recibo respuesta 404
    And el mensaje indica "Cliente not found"

  @P1 @Validation
  Scenario: Ticket no encontrado → 404
    When consulto GET /api/tickets/{uuid-inexistente}
    Then recibo respuesta 404

  @P1 @Validation
  Scenario Outline: Validar campos de ClienteRequest
    When envío POST /api/tickets/cliente con <campo> <valor>
    Then recibo respuesta <codigo>
    
    Examples:
      | campo       | valor           | codigo |
      | nationalId  | ""              | 400    |
      | nationalId  | "1234567"       | 400    | # Muy corto
      | nationalId  | "12345678"      | 201    | # Válido
      | nationalId  | "123456789012"  | 201    | # Válido
      | nationalId  | "1234567890123" | 400    | # Muy largo
      | nombre      | ""              | 400    |
      | nombre      | null            | 400    |

  @P1 @Validation
  Scenario: Cliente duplicado → 409
    Given existe un cliente con nationalId "12345678"
    When intento crear otro cliente con el mismo nationalId
    Then recibo respuesta 409
    And el mensaje indica "Cliente already exists"
```

---

## PASO 6: Feature - Dashboard Admin

**Objetivo:** Validar endpoints administrativos.

### Escenarios Gherkin
```gherkin
Feature: Dashboard Administrativo
  Como administrador
  Quiero ver el estado del sistema
  Para monitorear las operaciones

  @P2 @Admin
  Scenario: Ver dashboard general
    Given existen tickets en diferentes estados
    When consulto GET /api/admin/dashboard
    Then recibo resumen por cola y estadísticas de asesores

  @P2 @Admin
  Scenario: Ver estado de cola específica
    Given existen 5 tickets en cola CAJA
    When consulto GET /api/admin/queues/CAJA
    Then recibo lista de tickets activos en esa cola

  @P2 @Admin
  Scenario: Cambiar estado de asesor
    Given existe un asesor en estado AVAILABLE
    When envío PUT /api/admin/advisors/{id}/status?status=BREAK
    Then el asesor cambia a estado BREAK

  @P2 @Admin
  Scenario: Ver estadísticas de asesores
    Given hay asesores con tickets atendidos
    When consulto GET /api/admin/advisors/stats
    Then recibo total, disponibles, ocupados y tickets atendidos
```

---

## PASO 7: Ejecución Final y Reporte

**Objetivo:** Ejecutar todos los tests E2E y generar reporte.

### Comandos:
```bash
# 1. Ejecutar todos los tests de integración
mvn test -Dtest="*IT" 

# 2. Generar reporte HTML
mvn surefire-report:report

# 3. Ver reporte
start target/site/surefire-report.html
```

### Resultados Esperados:
```
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0

Tests por feature:
- TicketCreationIT: 6 tests
- TicketProcessingIT: 5 tests  
- NotificationIT: 4 tests
- ValidationIT: 5 tests
- AdminDashboardIT: 4 tests
```

---

## Diferencias Clave vs Prompt Original

### ❌ Removido (No aplica a CoopFila):
- RabbitMQ TestContainer
- Patrón Outbox
- Routing keys de RabbitMQ
- Validación de colas de mensajería

### ✅ Adaptado para CoopFila:
- Solo PostgreSQL TestContainer
- Modelo Cliente + Ticket (no nationalId directo)
- Mensajes programados (tabla `mensaje`)
- Procesamiento directo (sin workers de cola)
- Endpoints existentes (`/api/tickets`, `/api/tickets/cliente`)
- Estructura de DTOs actual (TicketRequest, ClienteRequest)

### 🔧 Configuración Específica:
- PostgreSQL 15 (no 16)
- Puerto WireMock 8089
- Perfil de test `application-test.yml`
- Limpieza de tablas: `cliente`, `ticket`, `mensaje`, `audit_log`, `advisor`

---

## Datos de Prueba CoopFila

### Formato chileno:
- **nationalId:** 8-12 dígitos (ej: "12345678")
- **telefono:** "+569XXXXXXXX" (ej: "+56912345678")
- **Sucursales:** "Sucursal Centro", "Sucursal Norte"

### Colas disponibles:
- **CAJA** (prefijo C)
- **PERSONAL** (prefijo P)  
- **EMPRESAS** (prefijo E)
- **GERENCIA** (prefijo G)

---

**¿Estás listo para comenzar con el PASO 1?**