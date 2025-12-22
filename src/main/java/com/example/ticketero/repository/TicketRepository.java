package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    Optional<Ticket> findByCodigoReferencia(UUID codigoReferencia);
    
    Optional<Ticket> findByNumero(String numero);
    
    List<Ticket> findByClienteIdAndStatus(Long clienteId, TicketStatus status);
    
    List<Ticket> findByQueueTypeAndStatus(QueueType queueType, TicketStatus status);
    
    @Query("""
        SELECT t FROM Ticket t 
        JOIN FETCH t.cliente 
        WHERE t.status = :status 
        ORDER BY t.createdAt ASC
        """)
    List<Ticket> findByStatusWithCliente(@Param("status") TicketStatus status);
    
    long countByQueueTypeAndStatus(QueueType queueType, TicketStatus status);
}