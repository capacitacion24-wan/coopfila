package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.Cliente;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.ClienteRepository;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService - Unit Tests")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private TicketService ticketService;

    // ============================================================
    // CREAR TICKET
    // ============================================================
    
    @Nested
    @DisplayName("create()")
    class CrearTicket {

        @Test
        @DisplayName("con datos válidos → debe crear ticket y programar mensajes")
        void create_conDatosValidos_debeCrearTicketYProgramarMensajes() {
            // Given
            TicketRequest request = ticketRequestValido();
            Cliente cliente = clienteValido().build();
            Ticket ticketGuardado = ticketEnEspera()
                .cliente(cliente)
                .numero("C01")
                .positionInQueue(1)
                .estimatedWaitMinutes(5)
                .build();

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.EN_ESPERA))
                .thenReturn(0L);
            when(ticketRepository.save(any(Ticket.class))).thenReturn(ticketGuardado);

            // When
            TicketResponse response = ticketService.create(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.numero()).isEqualTo("C01");
            assertThat(response.positionInQueue()).isEqualTo(1);
            assertThat(response.estimatedWaitMinutes()).isEqualTo(5);
            assertThat(response.status()).isEqualTo("EN_ESPERA");
            assertThat(response.clienteNombre()).isEqualTo("Juan Pérez");

            verify(mensajeService).scheduleMessages(ticketGuardado);
        }

        @Test
        @DisplayName("cola PERSONAL_BANKER → debe generar número correcto")
        void create_colaPersonalBanker_debeGenerarNumeroCorrecto() {
            // Given
            TicketRequest request = new TicketRequest(1L, "Sucursal Centro", QueueType.PERSONAL_BANKER);
            Cliente cliente = clienteValido().build();
            
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.PERSONAL_BANKER, TicketStatus.EN_ESPERA))
                .thenReturn(2L);
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket ticket = invocation.getArgument(0);
                ticket.setId(1L);
                return ticket;
            });

            // When
            TicketResponse response = ticketService.create(request);

            // Then
            assertThat(response.numero()).isEqualTo("P03");
        }

        @Test
        @DisplayName("debe calcular posición y tiempo estimado correctamente")
        void create_debeCalcularPosicionYTiempoCorrectamente() {
            // Given
            TicketRequest request = ticketRequestValido();
            Cliente cliente = clienteValido().build();
            
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.EN_ESPERA))
                .thenReturn(4L); // 4 tickets en espera
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket ticket = invocation.getArgument(0);
                ticket.setId(1L);
                return ticket;
            });

            // When
            TicketResponse response = ticketService.create(request);

            // Then
            assertThat(response.positionInQueue()).isEqualTo(5); // posición 5
            assertThat(response.estimatedWaitMinutes()).isEqualTo(25); // 5 * 5 minutos
        }

        @Test
        @DisplayName("cliente inexistente → debe lanzar RuntimeException")
        void create_clienteInexistente_debeLanzarRuntimeException() {
            // Given
            TicketRequest request = ticketRequestValido();
            when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> ticketService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cliente not found");

            verify(ticketRepository, never()).save(any());
            verify(mensajeService, never()).scheduleMessages(any());
        }

        @Test
        @DisplayName("debe guardar ticket con datos correctos")
        void create_debeGuardarTicketConDatosCorrectos() {
            // Given
            TicketRequest request = ticketRequestValido();
            Cliente cliente = clienteValido().build();
            
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(0L);
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket ticket = invocation.getArgument(0);
                ticket.setId(1L);
                // Simular @PrePersist
                if (ticket.getCodigoReferencia() == null) {
                    ticket.setCodigoReferencia(UUID.randomUUID());
                }
                return ticket;
            });

            // When
            ticketService.create(request);

            // Then
            ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(ticketCaptor.capture());

            Ticket savedTicket = ticketCaptor.getValue();
            assertThat(savedTicket.getCliente()).isEqualTo(cliente);
            assertThat(savedTicket.getBranchOffice()).isEqualTo("Sucursal Centro");
            assertThat(savedTicket.getQueueType()).isEqualTo(QueueType.CAJA);
            assertThat(savedTicket.getStatus()).isEqualTo(TicketStatus.EN_ESPERA);
            // No verificamos codigoReferencia aquí porque se genera en @PrePersist
        }
    }

    // ============================================================
    // OBTENER TICKET POR CÓDIGO
    // ============================================================
    
    @Nested
    @DisplayName("findByCodigoReferencia()")
    class ObtenerTicketPorCodigo {

        @Test
        @DisplayName("con UUID existente → debe retornar ticket")
        void findByCodigoReferencia_conUuidExistente_debeRetornarTicket() {
            // Given
            UUID codigo = UUID.randomUUID();
            Cliente cliente = clienteValido().nombre("Juan").apellido("Pérez").build();
            Ticket ticket = ticketEnEspera()
                .codigoReferencia(codigo)
                .numero("C01")
                .cliente(cliente)
                .build();

            when(ticketRepository.findByCodigoReferencia(codigo)).thenReturn(Optional.of(ticket));

            // When
            Optional<TicketResponse> response = ticketService.findByCodigoReferencia(codigo);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().codigoReferencia()).isEqualTo(codigo);
            assertThat(response.get().numero()).isEqualTo("C01");
            assertThat(response.get().clienteNombre()).isEqualTo("Juan Pérez");
        }

        @Test
        @DisplayName("con UUID inexistente → debe retornar Optional.empty()")
        void findByCodigoReferencia_conUuidInexistente_debeRetornarEmpty() {
            // Given
            UUID codigo = UUID.randomUUID();
            when(ticketRepository.findByCodigoReferencia(codigo)).thenReturn(Optional.empty());

            // When
            Optional<TicketResponse> response = ticketService.findByCodigoReferencia(codigo);

            // Then
            assertThat(response).isEmpty();
        }
    }

    // ============================================================
    // OBTENER TICKETS ACTIVOS
    // ============================================================
    
    @Nested
    @DisplayName("findActiveTickets()")
    class ObtenerTicketsActivos {

        @Test
        @DisplayName("con tickets en espera → debe retornar lista")
        void findActiveTickets_conTicketsEnEspera_debeRetornarLista() {
            // Given
            Cliente cliente = clienteValido().nombre("Juan").apellido("Pérez").build();
            Ticket ticket1 = ticketEnEspera().id(1L).numero("C01").cliente(cliente).build();
            Ticket ticket2 = ticketEnEspera().id(2L).numero("C02").cliente(cliente).build();

            when(ticketRepository.findByStatusWithCliente(TicketStatus.EN_ESPERA))
                .thenReturn(List.of(ticket1, ticket2));

            // When
            List<TicketResponse> response = ticketService.findActiveTickets();

            // Then
            assertThat(response).hasSize(2);
            assertThat(response.get(0).numero()).isEqualTo("C01");
            assertThat(response.get(1).numero()).isEqualTo("C02");
        }

        @Test
        @DisplayName("sin tickets activos → debe retornar lista vacía")
        void findActiveTickets_sinTicketsActivos_debeRetornarListaVacia() {
            // Given
            when(ticketRepository.findByStatusWithCliente(TicketStatus.EN_ESPERA))
                .thenReturn(List.of());

            // When
            List<TicketResponse> response = ticketService.findActiveTickets();

            // Then
            assertThat(response).isEmpty();
        }
    }
}