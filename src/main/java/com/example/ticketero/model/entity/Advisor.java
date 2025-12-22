package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.AdvisorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "advisor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvisorStatus status;

    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;

    @Column(name = "assigned_tickets_count", nullable = false)
    private Integer assignedTicketsCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "assignedAdvisor")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Ticket> assignedTickets = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.assignedTicketsCount == null) {
            this.assignedTicketsCount = 0;
        }
        if (this.status == null) {
            this.status = AdvisorStatus.AVAILABLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}