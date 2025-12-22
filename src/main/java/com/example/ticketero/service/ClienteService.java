package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.ClienteRequest;
import com.example.ticketero.model.dto.response.ClienteResponse;
import com.example.ticketero.model.entity.Cliente;
import com.example.ticketero.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponse create(ClienteRequest request) {
        log.info("Creating cliente with nationalId: {}", request.nationalId());
        
        Cliente cliente = Cliente.builder()
            .nationalId(request.nationalId())
            .nombre(request.nombre())
            .apellido(request.apellido())
            .telefono(request.telefono())
            .email(request.email())
            .fechaNacimiento(request.fechaNacimiento())
            .build();

        Cliente saved = clienteRepository.save(cliente);
        return toResponse(saved);
    }

    public Optional<ClienteResponse> findByNationalId(String nationalId) {
        return clienteRepository.findByNationalId(nationalId)
            .map(this::toResponse);
    }

    public Optional<ClienteResponse> findById(Long id) {
        return clienteRepository.findById(id)
            .map(this::toResponse);
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
            cliente.getId(),
            cliente.getNationalId(),
            cliente.getNombre(),
            cliente.getApellido(),
            cliente.getTelefono(),
            cliente.getEmail(),
            cliente.getFechaNacimiento(),
            cliente.getCreatedAt()
        );
    }
}