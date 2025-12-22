package com.example.ticketero.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 50)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cambios_estado", columnDefinition = "jsonb")
    private Map<String, Object> cambiosEstado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalles_adicionales", columnDefinition = "jsonb")
    private Map<String, Object> detallesAdicionales;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}