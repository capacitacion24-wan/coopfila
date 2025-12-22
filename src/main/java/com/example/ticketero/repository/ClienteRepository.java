package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    Optional<Cliente> findByNationalId(String nationalId);
    
    boolean existsByNationalId(String nationalId);
    
    Optional<Cliente> findByTelefono(String telefono);
}