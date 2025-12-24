package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.ClienteRequest;
import com.example.ticketero.model.dto.response.ClienteResponse;
import com.example.ticketero.model.entity.Cliente;
import com.example.ticketero.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - Unit Tests")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    // ============================================================
    // CREAR CLIENTE
    // ============================================================
    
    @Nested
    @DisplayName("create()")
    class CrearCliente {

        @Test
        @DisplayName("con datos válidos → debe crear cliente correctamente")
        void create_conDatosValidos_debeCrearClienteCorrectamente() {
            // Given
            ClienteRequest request = clienteRequestValido();
            Cliente clienteGuardado = clienteValido()
                .id(1L)
                .build();

            when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteGuardado);

            // When
            ClienteResponse response = clienteService.create(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.nationalId()).isEqualTo("12345678");
            assertThat(response.nombre()).isEqualTo("Juan");
            assertThat(response.apellido()).isEqualTo("Pérez");
            assertThat(response.telefono()).isEqualTo("+56912345678");
            assertThat(response.email()).isEqualTo("juan.perez@email.com");
            assertThat(response.fechaNacimiento()).isEqualTo(LocalDate.of(1990, 1, 1));
        }

        @Test
        @DisplayName("debe guardar cliente con datos del request")
        void create_debeGuardarClienteConDatosDelRequest() {
            // Given
            ClienteRequest request = new ClienteRequest(
                "87654321",
                "María",
                "González",
                "+56987654321",
                "maria.gonzalez@email.com",
                LocalDate.of(1985, 5, 15)
            );

            when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
                Cliente cliente = invocation.getArgument(0);
                cliente.setId(2L);
                return cliente;
            });

            // When
            ClienteResponse response = clienteService.create(request);

            // Then
            ArgumentCaptor<Cliente> clienteCaptor = ArgumentCaptor.forClass(Cliente.class);
            verify(clienteRepository).save(clienteCaptor.capture());

            Cliente clienteGuardado = clienteCaptor.getValue();
            assertThat(clienteGuardado.getNationalId()).isEqualTo("87654321");
            assertThat(clienteGuardado.getNombre()).isEqualTo("María");
            assertThat(clienteGuardado.getApellido()).isEqualTo("González");
            assertThat(clienteGuardado.getTelefono()).isEqualTo("+56987654321");
            assertThat(clienteGuardado.getEmail()).isEqualTo("maria.gonzalez@email.com");
            assertThat(clienteGuardado.getFechaNacimiento()).isEqualTo(LocalDate.of(1985, 5, 15));

            assertThat(response.nationalId()).isEqualTo("87654321");
            assertThat(response.nombre()).isEqualTo("María");
        }
    }

    // ============================================================
    // BUSCAR POR NATIONAL ID
    // ============================================================
    
    @Nested
    @DisplayName("findByNationalId()")
    class BuscarPorNationalId {

        @Test
        @DisplayName("con nationalId existente → debe retornar cliente")
        void findByNationalId_conNationalIdExistente_debeRetornarCliente() {
            // Given
            String nationalId = "12345678";
            Cliente cliente = clienteValido()
                .nationalId(nationalId)
                .build();

            when(clienteRepository.findByNationalId(nationalId)).thenReturn(Optional.of(cliente));

            // When
            Optional<ClienteResponse> response = clienteService.findByNationalId(nationalId);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().nationalId()).isEqualTo(nationalId);
            assertThat(response.get().nombre()).isEqualTo("Juan");
            assertThat(response.get().apellido()).isEqualTo("Pérez");
        }

        @Test
        @DisplayName("con nationalId inexistente → debe retornar Optional.empty()")
        void findByNationalId_conNationalIdInexistente_debeRetornarEmpty() {
            // Given
            String nationalId = "99999999";
            when(clienteRepository.findByNationalId(nationalId)).thenReturn(Optional.empty());

            // When
            Optional<ClienteResponse> response = clienteService.findByNationalId(nationalId);

            // Then
            assertThat(response).isEmpty();
        }
    }

    // ============================================================
    // BUSCAR POR ID
    // ============================================================
    
    @Nested
    @DisplayName("findById()")
    class BuscarPorId {

        @Test
        @DisplayName("con ID existente → debe retornar cliente")
        void findById_conIdExistente_debeRetornarCliente() {
            // Given
            Long clienteId = 1L;
            Cliente cliente = clienteValido()
                .id(clienteId)
                .build();

            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

            // When
            Optional<ClienteResponse> response = clienteService.findById(clienteId);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().id()).isEqualTo(clienteId);
            assertThat(response.get().nombre()).isEqualTo("Juan");
        }

        @Test
        @DisplayName("con ID inexistente → debe retornar Optional.empty()")
        void findById_conIdInexistente_debeRetornarEmpty() {
            // Given
            Long clienteId = 999L;
            when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

            // When
            Optional<ClienteResponse> response = clienteService.findById(clienteId);

            // Then
            assertThat(response).isEmpty();
        }
    }

    // ============================================================
    // MAPEO A RESPONSE
    // ============================================================
    
    @Nested
    @DisplayName("toResponse() mapping")
    class MapeoAResponse {

        @Test
        @DisplayName("debe mapear correctamente todos los campos")
        void toResponse_debeMapeareCorrectamenteTodosLosCampos() {
            // Given
            Cliente cliente = clienteValido()
                .id(5L)
                .nationalId("11111111")
                .nombre("Ana")
                .apellido("Martínez")
                .telefono("+56911111111")
                .email("ana.martinez@email.com")
                .fechaNacimiento(LocalDate.of(1992, 12, 25))
                .build();

            when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));

            // When
            Optional<ClienteResponse> response = clienteService.findById(5L);

            // Then
            assertThat(response).isPresent();
            ClienteResponse clienteResponse = response.get();
            
            assertThat(clienteResponse.id()).isEqualTo(5L);
            assertThat(clienteResponse.nationalId()).isEqualTo("11111111");
            assertThat(clienteResponse.nombre()).isEqualTo("Ana");
            assertThat(clienteResponse.apellido()).isEqualTo("Martínez");
            assertThat(clienteResponse.telefono()).isEqualTo("+56911111111");
            assertThat(clienteResponse.email()).isEqualTo("ana.martinez@email.com");
            assertThat(clienteResponse.fechaNacimiento()).isEqualTo(LocalDate.of(1992, 12, 25));
            assertThat(clienteResponse.createdAt()).isNotNull();
        }

        @Test
        @DisplayName("con campos nulos opcionales debe mapear correctamente")
        void toResponse_conCamposNulosOpcionales_debeMapeareCorrectamente() {
            // Given
            Cliente cliente = clienteValido()
                .telefono(null)
                .email(null)
                .build();

            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

            ClienteRequest request = new ClienteRequest(
                "12345678",
                "Juan",
                "Pérez",
                null, // telefono null
                null, // email null
                LocalDate.of(1990, 1, 1)
            );

            // When
            ClienteResponse response = clienteService.create(request);

            // Then
            assertThat(response.telefono()).isNull();
            assertThat(response.email()).isNull();
            assertThat(response.nombre()).isEqualTo("Juan");
            assertThat(response.apellido()).isEqualTo("Pérez");
        }
    }
}