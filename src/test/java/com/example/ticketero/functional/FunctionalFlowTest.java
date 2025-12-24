package com.example.ticketero.functional;

import com.example.ticketero.model.dto.request.ClienteRequest;
import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.ClienteResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.repository.ClienteRepository;
import com.example.ticketero.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Functional Tests - Flujo Completo")
class FunctionalFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Flujo completo: crear cliente → crear ticket → consultar ticket")
    void flujoCompleto_crearClienteYTicket_debeCompletarseCorrectamente() throws Exception {
        // 1. Crear cliente
        ClienteRequest clienteRequest = new ClienteRequest(
            "12345678",
            "Juan",
            "Pérez",
            "+56912345678",
            "juan@example.com",
            null
        );

        String clienteResponseJson = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clienteRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Juan"))
            .andExpect(jsonPath("$.apellido").value("Pérez"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        ClienteResponse clienteResponse = objectMapper.readValue(clienteResponseJson, ClienteResponse.class);

        // 2. Crear ticket para el cliente
        TicketRequest ticketRequest = new TicketRequest(
            clienteResponse.id(),
            "Sucursal Centro",
            QueueType.CAJA
        );

        String ticketResponseJson = mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value("C001"))
            .andExpect(jsonPath("$.status").value("EN_ESPERA"))
            .andExpect(jsonPath("$.positionInQueue").value(1))
            .andExpect(jsonPath("$.clienteName").value("Juan Pérez"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        TicketResponse ticketResponse = objectMapper.readValue(ticketResponseJson, TicketResponse.class);

        // 3. Consultar ticket por código de referencia
        mockMvc.perform(get("/api/tickets/{codigo}", ticketResponse.codigoReferencia()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero").value("C001"))
            .andExpect(jsonPath("$.status").value("EN_ESPERA"))
            .andExpect(jsonPath("$.clienteName").value("Juan Pérez"));

        // 4. Verificar que se crearon los registros en BD
        assertThat(clienteRepository.count()).isEqualTo(1);
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Múltiples tickets en cola → debe calcular posiciones correctamente")
    void multiplesTicketsEnCola_debeCalcularPosicionesCorrectamente() throws Exception {
        // Crear cliente
        ClienteRequest clienteRequest = new ClienteRequest("12345678", "Juan", "Pérez", "+56912345678", "juan@example.com", null);
        String clienteJson = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clienteRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        
        ClienteResponse cliente = objectMapper.readValue(clienteJson, ClienteResponse.class);

        // Crear 3 tickets
        for (int i = 1; i <= 3; i++) {
            TicketRequest ticketRequest = new TicketRequest(cliente.id(), "Sucursal Centro", QueueType.CAJA);
            
            mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ticketRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("C00" + i))
                .andExpect(jsonPath("$.positionInQueue").value(i))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(i * 5));
        }

        // Verificar tickets activos
        mockMvc.perform(get("/api/tickets/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].numero").value("C001"))
            .andExpect(jsonPath("$[1].numero").value("C002"))
            .andExpect(jsonPath("$[2].numero").value("C003"));
    }

    @Test
    @DisplayName("Diferentes tipos de cola → debe generar números correctos")
    void diferentesTiposCola_debeGenerarNumerosCorrectos() throws Exception {
        // Crear cliente
        ClienteRequest clienteRequest = new ClienteRequest("12345678", "Juan", "Pérez", "+56912345678", "juan@example.com", null);
        String clienteJson = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clienteRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        
        ClienteResponse cliente = objectMapper.readValue(clienteJson, ClienteResponse.class);

        // Ticket CAJA
        TicketRequest cajaRequest = new TicketRequest(cliente.id(), "Sucursal Centro", QueueType.CAJA);
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cajaRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value("C001"));

        // Ticket PERSONAL_BANKER
        TicketRequest personalRequest = new TicketRequest(cliente.id(), "Sucursal Centro", QueueType.PERSONAL_BANKER);
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(personalRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value("P001"));

        // Ticket EMPRESAS
        TicketRequest empresasRequest = new TicketRequest(cliente.id(), "Sucursal Centro", QueueType.EMPRESAS);
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(empresasRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").value("E001"));
    }
}