package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_referencia", unique = true, nullable = false)
    private UUID codigoReferencia;

    @Column(unique = true, nullable = false, length = 10)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @ToString.Exclude
    private Cliente cliente;

    @Column(name = "branch_office", nullable = false, length = 100)
    private String branchOffice;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 20)
    private QueueType queueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "position_in_queue", nullable = false)
    private Integer positionInQueue;

    @Column(name = "estimated_wait_minutes", nullable = false)
    private Integer estimatedWaitMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_advisor_id")
    @ToString.Exclude
    private Advisor assignedAdvisor;

    @Column(name = "assigned_module_number")
    private Integer assignedModuleNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Mensaje> mensajes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.codigoReferencia == null) {
            this.codigoReferencia = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}