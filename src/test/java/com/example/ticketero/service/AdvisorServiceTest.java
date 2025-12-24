package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.repository.AdvisorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdvisorService - Unit Tests")
class AdvisorServiceTest {

    @Mock
    private AdvisorRepository advisorRepository;

    @InjectMocks
    private AdvisorService advisorService;

    // ============================================================
    // OBTENER ADVISORS DISPONIBLES
    // ============================================================
    
    @Nested
    @DisplayName("findAvailable()")
    class ObtenerAdvisorsDisponibles {

        @Test
        @DisplayName("con advisors disponibles → debe retornar lista")
        void findAvailable_conAdvisorsDisponibles_debeRetornarLista() {
            // Given
            Advisor advisor1 = advisorDisponible().id(1L).name("María López").build();
            Advisor advisor2 = advisorDisponible().id(2L).name("Juan García").build();
            
            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(List.of(advisor1, advisor2));

            // When
            List<AdvisorResponse> response = advisorService.findAvailable();

            // Then
            assertThat(response).hasSize(2);
            assertThat(response.get(0).name()).isEqualTo("María López");
            assertThat(response.get(1).name()).isEqualTo("Juan García");
            assertThat(response.get(0).status()).isEqualTo("AVAILABLE");
        }

        @Test
        @DisplayName("sin advisors disponibles → debe retornar lista vacía")
        void findAvailable_sinAdvisorsDisponibles_debeRetornarListaVacia() {
            // Given
            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(Collections.emptyList());

            // When
            List<AdvisorResponse> response = advisorService.findAvailable();

            // Then
            assertThat(response).isEmpty();
        }
    }

    // ============================================================
    // OBTENER MEJOR ADVISOR DISPONIBLE
    // ============================================================
    
    @Nested
    @DisplayName("findBestAvailableAdvisor()")
    class ObtenerMejorAdvisorDisponible {

        @Test
        @DisplayName("con advisors disponibles → debe retornar el menos ocupado")
        void findBestAvailable_conAdvisorsDisponibles_debeRetornarMenosOcupado() {
            // Given
            Advisor advisorMenosOcupado = advisorDisponible()
                .assignedTicketsCount(1)
                .build();
            Advisor advisorMasOcupado = advisorDisponible()
                .assignedTicketsCount(5)
                .build();
            
            // Repository devuelve ordenado por assignedTicketsCount ASC
            when(advisorRepository.findAvailableOrderByAssignedCount())
                .thenReturn(List.of(advisorMenosOcupado, advisorMasOcupado));

            // When
            Optional<Advisor> result = advisorService.findBestAvailableAdvisor();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(advisorMenosOcupado);
            assertThat(result.get().getAssignedTicketsCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("sin advisors disponibles → debe retornar Optional.empty()")
        void findBestAvailable_sinAdvisorsDisponibles_debeRetornarEmpty() {
            // Given
            when(advisorRepository.findAvailableOrderByAssignedCount())
                .thenReturn(Collections.emptyList());

            // When
            Optional<Advisor> result = advisorService.findBestAvailableAdvisor();

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // ASIGNAR TICKET
    // ============================================================
    
    @Nested
    @DisplayName("assignTicket()")
    class AsignarTicket {

        @Test
        @DisplayName("con advisor válido → debe incrementar contador y cambiar estado")
        void assignTicket_conAdvisorValido_debeIncrementarContadorYCambiarEstado() {
            // Given
            Advisor advisor = advisorDisponible()
                .assignedTicketsCount(2)
                .status(AdvisorStatus.AVAILABLE)
                .build();
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.assignTicket(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(3);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.BUSY);
            
            verify(advisorRepository).findById(1L);
        }

        @Test
        @DisplayName("advisor inexistente → debe lanzar RuntimeException")
        void assignTicket_advisorInexistente_debeLanzarRuntimeException() {
            // Given
            when(advisorRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> advisorService.assignTicket(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Advisor not found");
        }

        @Test
        @DisplayName("debe incrementar desde cero correctamente")
        void assignTicket_debeIncrementarDesdeCeroCorrectamente() {
            // Given
            Advisor advisor = advisorDisponible()
                .assignedTicketsCount(0)
                .build();
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.assignTicket(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(1);
        }
    }

    // ============================================================
    // COMPLETAR TICKET
    // ============================================================
    
    @Nested
    @DisplayName("completeTicket()")
    class CompletarTicket {

        @Test
        @DisplayName("con advisor ocupado → debe decrementar contador y cambiar estado")
        void completeTicket_conAdvisorOcupado_debeDecrementarContadorYCambiarEstado() {
            // Given
            Advisor advisor = advisorOcupado()
                .assignedTicketsCount(3)
                .status(AdvisorStatus.BUSY)
                .build();
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.completeTicket(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(2);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
        }

        @Test
        @DisplayName("con contador en cero → no debe ser negativo")
        void completeTicket_conContadorEnCero_noDebeSerNegativo() {
            // Given
            Advisor advisor = advisorOcupado()
                .assignedTicketsCount(0)
                .build();
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.completeTicket(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(0);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
        }

        @Test
        @DisplayName("advisor inexistente → debe lanzar RuntimeException")
        void completeTicket_advisorInexistente_debeLanzarRuntimeException() {
            // Given
            when(advisorRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> advisorService.completeTicket(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Advisor not found");
        }

        @Test
        @DisplayName("con un ticket asignado → debe quedar disponible")
        void completeTicket_conUnTicketAsignado_debeQuedarDisponible() {
            // Given
            Advisor advisor = advisorOcupado()
                .assignedTicketsCount(1)
                .build();
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.completeTicket(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(0);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
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
            Advisor advisor = advisorDisponible()
                .id(5L)
                .name("Carlos Mendoza")
                .email("carlos.mendoza@banco.com")
                .status(AdvisorStatus.AVAILABLE)
                .moduleNumber(3)
                .assignedTicketsCount(2)
                .build();
            
            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(List.of(advisor));

            // When
            List<AdvisorResponse> responses = advisorService.findAvailable();

            // Then
            assertThat(responses).hasSize(1);
            AdvisorResponse response = responses.get(0);
            
            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.name()).isEqualTo("Carlos Mendoza");
            assertThat(response.email()).isEqualTo("carlos.mendoza@banco.com");
            assertThat(response.status()).isEqualTo("AVAILABLE");
            assertThat(response.moduleNumber()).isEqualTo(3);
            assertThat(response.assignedTicketsCount()).isEqualTo(2);
            assertThat(response.createdAt()).isNotNull();
        }
    }
}