package com.example.ticketero.integration;

import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.Cliente;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.repository.ClienteRepository;
import com.example.ticketero.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.example.ticketero.testutil.TestDataBuilder.clienteValido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
@Transactional
@DisplayName("Ticket Integration Tests")
class TicketIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(clienteValido().build());
    }

    @Test
    @DisplayName("POST /api/tickets - crear ticket válido")
    void createTicket_conDatosValidos_debeCrearTicket() throws Exception {
        // Given
        TicketRequest request = new TicketRequest(
            cliente.getId(),
            "Sucursal Centro",
            QueueType.CAJA
        );

        // When & Then
        String responseJson = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value("C001"))
            .andExpect(jsonPath("$.status").value("EN_ESPERA"))
            .andExpect(jsonPath("$.positionInQueue").value(1))
            .andExpect(jsonPath("$.clienteNombre").value("Juan Pérez"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        TicketResponse response = objectMapper.readValue(responseJson, TicketResponse.class);
        assertThat(response.codigoReferencia()).isNotNull();
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/tickets/{codigo} - obtener ticket existente")
    void getTicket_conCodigoValido_debeRetornarTicket() throws Exception {
        // Given - crear ticket primero
        TicketRequest request = new TicketRequest(cliente.getId(), "Sucursal Centro", QueueType.CAJA);
        
        String createResponse = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        TicketResponse createdTicket = objectMapper.readValue(createResponse, TicketResponse.class);

        // When & Then
        mockMvc.perform(get("/api/tickets/{codigo}", createdTicket.codigoReferencia()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero").value("C001"))
            .andExpect(jsonPath("$.status").value("EN_ESPERA"));
    }

    @Test
    @DisplayName("POST /api/tickets - cliente inexistente debe retornar 400")
    void createTicket_clienteInexistente_debeRetornar400() throws Exception {
        // Given
        TicketRequest request = new TicketRequest(999L, "Sucursal Centro", QueueType.CAJA);

        // When & Then
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }
}