package com.example.ticketero.service;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageStatus;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MensajeService - Unit Tests")
class MensajeServiceTest {

    @Mock
    private MensajeRepository mensajeRepository;

    @InjectMocks
    private MensajeService mensajeService;

    // ============================================================
    // PROGRAMAR MENSAJES
    // ============================================================
    
    @Nested
    @DisplayName("scheduleMessages()")
    class ProgramarMensajes {

        @Test
        @DisplayName("debe crear 3 mensajes programados para ticket")
        void scheduleMessages_debeCrear3MensajesProgramados() {
            // Given
            Ticket ticket = ticketEnEspera()
                .numero("C01")
                .estimatedWaitMinutes(15)
                .build();

            // When
            mensajeService.scheduleMessages(ticket);

            // Then
            ArgumentCaptor<Mensaje> mensajeCaptor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository, times(3)).save(mensajeCaptor.capture());

            List<Mensaje> mensajesGuardados = mensajeCaptor.getAllValues();
            
            // Verificar que se crearon los 3 tipos de mensaje
            assertThat(mensajesGuardados).hasSize(3);
            assertThat(mensajesGuardados)
                .extracting(Mensaje::getPlantilla)
                .containsExactlyInAnyOrder(
                    MessageTemplate.TOTEM_TICKET_CREADO,
                    MessageTemplate.TOTEM_PROXIMO_TURNO,
                    MessageTemplate.TOTEM_ES_TU_TURNO
                );
        }

        @Test
        @DisplayName("mensaje inmediato debe programarse para ahora")
        void scheduleMessages_mensajeInmediato_debeProgramarseParaAhora() {
            // Given
            Ticket ticket = ticketEnEspera().build();
            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

            // When
            mensajeService.scheduleMessages(ticket);

            // Then
            ArgumentCaptor<Mensaje> mensajeCaptor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository, times(3)).save(mensajeCaptor.capture());

            Mensaje mensajeInmediato = mensajeCaptor.getAllValues().stream()
                .filter(m -> m.getPlantilla() == MessageTemplate.TOTEM_TICKET_CREADO)
                .findFirst()
                .orElseThrow();

            assertThat(mensajeInmediato.getFechaProgramada()).isAfter(antes);
            assertThat(mensajeInmediato.getFechaProgramada()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        }

        @Test
        @DisplayName("mensaje próximo turno debe programarse 5 minutos antes")
        void scheduleMessages_mensajeProximoTurno_debeProgramarse5MinutesAntes() {
            // Given
            Ticket ticket = ticketEnEspera()
                .estimatedWaitMinutes(15)
                .build();

            // When
            mensajeService.scheduleMessages(ticket);

            // Then
            ArgumentCaptor<Mensaje> mensajeCaptor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository, times(3)).save(mensajeCaptor.capture());

            Mensaje mensajeProximo = mensajeCaptor.getAllValues().stream()
                .filter(m -> m.getPlantilla() == MessageTemplate.TOTEM_PROXIMO_TURNO)
                .findFirst()
                .orElseThrow();

            // Debe programarse en 10 minutos (15 - 5)
            LocalDateTime expectedTime = LocalDateTime.now().plusMinutes(10);
            assertThat(mensajeProximo.getFechaProgramada())
                .isCloseTo(expectedTime, within(1, java.time.temporal.ChronoUnit.MINUTES));
        }

        @Test
        @DisplayName("con tiempo estimado menor a 5 minutos debe programar próximo turno para ahora")
        void scheduleMessages_conTiempoMenorA5Minutos_debeProgamarProximoParaAhora() {
            // Given
            Ticket ticket = ticketEnEspera()
                .estimatedWaitMinutes(3)
                .build();

            // When
            mensajeService.scheduleMessages(ticket);

            // Then
            ArgumentCaptor<Mensaje> mensajeCaptor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository, times(3)).save(mensajeCaptor.capture());

            Mensaje mensajeProximo = mensajeCaptor.getAllValues().stream()
                .filter(m -> m.getPlantilla() == MessageTemplate.TOTEM_PROXIMO_TURNO)
                .findFirst()
                .orElseThrow();

            // Debe programarse para ahora (max(0, 3-5) = 0)
            assertThat(mensajeProximo.getFechaProgramada())
                .isCloseTo(LocalDateTime.now(), within(1, java.time.temporal.ChronoUnit.MINUTES));
        }
    }

    // ============================================================
    // OBTENER MENSAJES PENDIENTES
    // ============================================================
    
    @Nested
    @DisplayName("findPendingMessages()")
    class ObtenerMensajesPendientes {

        @Test
        @DisplayName("debe retornar mensajes pendientes programados")
        void findPendingMessages_debeRetornarMensajesPendientesProgramados() {
            // Given
            Mensaje mensaje1 = mensajePendiente().id(1L).build();
            Mensaje mensaje2 = mensajePendiente().id(2L).build();
            
            when(mensajeRepository.findPendingMessages(eq(MessageStatus.PENDIENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(mensaje1, mensaje2));

            // When
            List<Mensaje> result = mensajeService.findPendingMessages();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(mensaje1, mensaje2);
        }

        @Test
        @DisplayName("sin mensajes pendientes debe retornar lista vacía")
        void findPendingMessages_sinMensajesPendientes_debeRetornarListaVacia() {
            // Given
            when(mensajeRepository.findPendingMessages(eq(MessageStatus.PENDIENTE), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

            // When
            List<Mensaje> result = mensajeService.findPendingMessages();

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // MARCAR COMO ENVIADO
    // ============================================================
    
    @Nested
    @DisplayName("markAsSent()")
    class MarcarComoEnviado {

        @Test
        @DisplayName("debe actualizar estado y fecha de envío")
        void markAsSent_debeActualizarEstadoYFechaEnvio() {
            // Given
            Mensaje mensaje = mensajePendiente()
                .estadoEnvio(MessageStatus.PENDIENTE)
                .fechaEnvio(null)
                .telegramMessageId(null)
                .build();
            
            when(mensajeRepository.findById(1L)).thenReturn(Optional.of(mensaje));

            // When
            mensajeService.markAsSent(1L, "12345");

            // Then
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(MessageStatus.ENVIADO);
            assertThat(mensaje.getFechaEnvio()).isNotNull();
            assertThat(mensaje.getTelegramMessageId()).isEqualTo("12345");
        }

        @Test
        @DisplayName("mensaje inexistente debe lanzar RuntimeException")
        void markAsSent_mensajeInexistente_debeLanzarRuntimeException() {
            // Given
            when(mensajeRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> mensajeService.markAsSent(999L, "12345"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mensaje not found");
        }
    }

    // ============================================================
    // MARCAR COMO FALLIDO
    // ============================================================
    
    @Nested
    @DisplayName("markAsFailed()")
    class MarcarComoFallido {

        @Test
        @DisplayName("primer intento fallido debe incrementar contador")
        void markAsFailed_primerIntentoFallido_debeIncrementarContador() {
            // Given
            Mensaje mensaje = mensajePendiente()
                .intentos(0)
                .fechaProgramada(LocalDateTime.now().minusMinutes(5))
                .build();
            
            when(mensajeRepository.findById(1L)).thenReturn(Optional.of(mensaje));

            // When
            mensajeService.markAsFailed(1L);

            // Then
            assertThat(mensaje.getIntentos()).isEqualTo(1);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(MessageStatus.PENDIENTE);
            assertThat(mensaje.getFechaProgramada()).isAfter(LocalDateTime.now().plusSeconds(25));
        }

        @Test
        @DisplayName("segundo intento fallido debe reprogramar con backoff")
        void markAsFailed_segundoIntentoFallido_debeReprogramarConBackoff() {
            // Given
            Mensaje mensaje = mensajePendiente()
                .intentos(1)
                .build();
            
            when(mensajeRepository.findById(1L)).thenReturn(Optional.of(mensaje));

            // When
            mensajeService.markAsFailed(1L);

            // Then
            assertThat(mensaje.getIntentos()).isEqualTo(2);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(MessageStatus.PENDIENTE);
            // Backoff: 30 * 2 = 60 segundos
            assertThat(mensaje.getFechaProgramada()).isAfter(LocalDateTime.now().plusSeconds(55));
        }

        @Test
        @DisplayName("tercer intento fallido debe marcar como FALLIDO")
        void markAsFailed_tercerIntentoFallido_debeMarcarComoFallido() {
            // Given
            Mensaje mensaje = mensajePendiente()
                .intentos(2)
                .build();
            
            when(mensajeRepository.findById(1L)).thenReturn(Optional.of(mensaje));

            // When
            mensajeService.markAsFailed(1L);

            // Then
            assertThat(mensaje.getIntentos()).isEqualTo(3);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(MessageStatus.FALLIDO);
        }

        @Test
        @DisplayName("mensaje inexistente debe lanzar RuntimeException")
        void markAsFailed_mensajeInexistente_debeLanzarRuntimeException() {
            // Given
            when(mensajeRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> mensajeService.markAsFailed(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mensaje not found");
        }
    }
}