package com.example.ticketero.repository;

import com.example.ticketero.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    List<AuditLog> findByTipoEvento(String tipoEvento);
    
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.timestamp BETWEEN :start AND :end 
        ORDER BY a.timestamp DESC
        """)
    List<AuditLog> findByTimestampBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}