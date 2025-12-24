package com.example.ticketero.service;

import com.example.ticketero.model.entity.AuditLog;
import com.example.ticketero.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService - Unit Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    // ============================================================
    // LOG EVENT GENÉRICO
    // ============================================================
    
    @Nested
    @DisplayName("logEvent()")
    class LogEventGenerico {

        @Test
        @DisplayName("debe crear y guardar AuditLog con todos los datos")
        void logEvent_debeCrearYGuardarAuditLogConTodosLosDatos() {
            // Given
            String tipoEvento = "TEST_EVENT";
            String actor = "TEST_USER";
            String entityType = "TestEntity";
            String entityId = "123";
            Map<String, Object> cambiosEstado = Map.of(
                "oldValue", "old",
                "newValue", "new"
            );

            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

            // When
            auditService.logEvent(tipoEvento, actor, entityType, entityId, cambiosEstado);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getTipoEvento()).isEqualTo("TEST_EVENT");
            assertThat(auditLog.getActor()).isEqualTo("TEST_USER");
            assertThat(auditLog.getEntityType()).isEqualTo("TestEntity");
            assertThat(auditLog.getEntityId()).isEqualTo("123");
            assertThat(auditLog.getCambiosEstado()).isEqualTo(cambiosEstado);
            assertThat(auditLog.getTimestamp()).isAfter(antes);
            assertThat(auditLog.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        }

        @Test
        @DisplayName("con cambios estado vacío debe funcionar correctamente")
        void logEvent_conCambiosEstadoVacio_debeFuncionarCorrectamente() {
            // Given
            Map<String, Object> cambiosVacios = Map.of();

            // When
            auditService.logEvent("EMPTY_EVENT", "USER", "Entity", "1", cambiosVacios);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getCambiosEstado()).isEmpty();
        }

        @Test
        @DisplayName("con cambios estado null debe funcionar correctamente")
        void logEvent_conCambiosEstadoNull_debeFuncionarCorrectamente() {
            // When
            auditService.logEvent("NULL_EVENT", "USER", "Entity", "1", null);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getCambiosEstado()).isNull();
        }
    }

    // ============================================================
    // LOG TICKET CREATED
    // ============================================================
    
    @Nested
    @DisplayName("logTicketCreated()")
    class LogTicketCreated {

        @Test
        @DisplayName("debe registrar evento de ticket creado correctamente")
        void logTicketCreated_debeRegistrarEventoCorrectamente() {
            // Given
            Long ticketId = 123L;
            String clienteNationalId = "12345678";

            // When
            auditService.logTicketCreated(ticketId, clienteNationalId);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getTipoEvento()).isEqualTo("TICKET_CREADO");
            assertThat(auditLog.getActor()).isEqualTo("SYSTEM");
            assertThat(auditLog.getEntityType()).isEqualTo("Ticket");
            assertThat(auditLog.getEntityId()).isEqualTo("123");
            
            Map<String, Object> cambios = auditLog.getCambiosEstado();
            assertThat(cambios).containsEntry("cliente", "12345678");
            assertThat(cambios).containsEntry("status", "EN_ESPERA");
        }

        @Test
        @DisplayName("con ticketId diferente debe usar ID correcto")
        void logTicketCreated_conTicketIdDiferente_debeUsarIdCorrecto() {
            // Given
            Long ticketId = 999L;
            String clienteNationalId = "87654321";

            // When
            auditService.logTicketCreated(ticketId, clienteNationalId);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getEntityId()).isEqualTo("999");
            
            Map<String, Object> cambios = auditLog.getCambiosEstado();
            assertThat(cambios).containsEntry("cliente", "87654321");
        }
    }

    // ============================================================
    // LOG TICKET STATUS CHANGE
    // ============================================================
    
    @Nested
    @DisplayName("logTicketStatusChange()")
    class LogTicketStatusChange {

        @Test
        @DisplayName("debe registrar cambio de estado correctamente")
        void logTicketStatusChange_debeRegistrarCambioCorrectamente() {
            // Given
            Long ticketId = 456L;
            String oldStatus = "EN_ESPERA";
            String newStatus = "ATENDIENDO";

            // When
            auditService.logTicketStatusChange(ticketId, oldStatus, newStatus);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getTipoEvento()).isEqualTo("TICKET_STATUS_CHANGED");
            assertThat(auditLog.getActor()).isEqualTo("SYSTEM");
            assertThat(auditLog.getEntityType()).isEqualTo("Ticket");
            assertThat(auditLog.getEntityId()).isEqualTo("456");
            
            Map<String, Object> cambios = auditLog.getCambiosEstado();
            assertThat(cambios).containsEntry("oldStatus", "EN_ESPERA");
            assertThat(cambios).containsEntry("newStatus", "ATENDIENDO");
        }

        @Test
        @DisplayName("con diferentes estados debe registrar correctamente")
        void logTicketStatusChange_conDiferentesEstados_debeRegistrarCorrectamente() {
            // Given
            Long ticketId = 789L;
            String oldStatus = "ATENDIENDO";
            String newStatus = "COMPLETADO";

            // When
            auditService.logTicketStatusChange(ticketId, oldStatus, newStatus);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getEntityId()).isEqualTo("789");
            
            Map<String, Object> cambios = auditLog.getCambiosEstado();
            assertThat(cambios).containsEntry("oldStatus", "ATENDIENDO");
            assertThat(cambios).containsEntry("newStatus", "COMPLETADO");
        }
    }

    // ============================================================
    // LOG MESSAGE SENT
    // ============================================================
    
    @Nested
    @DisplayName("logMessageSent()")
    class LogMessageSent {

        @Test
        @DisplayName("debe registrar mensaje enviado correctamente")
        void logMessageSent_debeRegistrarMensajeEnviadoCorrectamente() {
            // Given
            Long mensajeId = 111L;
            String template = "TOTEM_TICKET_CREADO";

            // When
            auditService.logMessageSent(mensajeId, template);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getTipoEvento()).isEqualTo("MESSAGE_SENT");
            assertThat(auditLog.getActor()).isEqualTo("TELEGRAM_BOT");
            assertThat(auditLog.getEntityType()).isEqualTo("Mensaje");
            assertThat(auditLog.getEntityId()).isEqualTo("111");
            
            Map<String, Object> cambios = auditLog.getCambiosEstado();
            assertThat(cambios).containsEntry("template", "TOTEM_TICKET_CREADO");
            assertThat(cambios).containsEntry("status", "ENVIADO");
        }

        @Test
        @DisplayName("con diferentes templates debe registrar correctamente")
        void logMessageSent_conDiferentesTemplates_debeRegistrarCorrectamente() {
            // Given
            Long mensajeId = 222L;
            String template = "TOTEM_ES_TU_TURNO";

            // When
            auditService.logMessageSent(mensajeId, template);

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getEntityId()).isEqualTo("222");
            
            Map<String, Object> cambios = auditLog.getCambiosEstado();
            assertThat(cambios).containsEntry("template", "TOTEM_ES_TU_TURNO");
        }
    }

    // ============================================================
    // VERIFICACIONES GENERALES
    // ============================================================
    
    @Nested
    @DisplayName("General Verifications")
    class VerificacionesGenerales {

        @Test
        @DisplayName("todos los métodos deben llamar al repositorio una vez")
        void todosLosMetodos_debenLlamarAlRepositorioUnaVez() {
            // When
            auditService.logTicketCreated(1L, "12345678");
            auditService.logTicketStatusChange(2L, "OLD", "NEW");
            auditService.logMessageSent(3L, "TEMPLATE");

            // Then
            verify(auditLogRepository, times(3)).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("timestamp debe ser reciente en todos los casos")
        void timestamp_debeSerRecienteEnTodosLosCasos() {
            // Given
            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

            // When
            auditService.logTicketCreated(1L, "12345678");

            // Then
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());

            AuditLog auditLog = auditCaptor.getValue();
            assertThat(auditLog.getTimestamp()).isAfter(antes);
            assertThat(auditLog.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        }
    }
}