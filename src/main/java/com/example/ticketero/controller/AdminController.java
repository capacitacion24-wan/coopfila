package com.example.ticketero.controller;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.service.AdvisorService;
import com.example.ticketero.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final TicketService ticketService;
    private final AdvisorService advisorService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        log.info("Getting admin dashboard");
        
        List<TicketResponse> activeTickets = ticketService.findActiveTickets();
        List<AdvisorResponse> availableAdvisors = advisorService.findAvailable();
        
        Map<String, Object> dashboard = Map.of(
            "activeTickets", activeTickets,
            "totalActiveTickets", activeTickets.size(),
            "availableAdvisors", availableAdvisors,
            "totalAvailableAdvisors", availableAdvisors.size()
        );
        
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/tickets/active")
    public ResponseEntity<List<TicketResponse>> getActiveTickets() {
        List<TicketResponse> tickets = ticketService.findActiveTickets();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/advisors/available")
    public ResponseEntity<List<AdvisorResponse>> getAvailableAdvisors() {
        List<AdvisorResponse> advisors = advisorService.findAvailable();
        return ResponseEntity.ok(advisors);
    }

    @PostMapping("/advisors/{advisorId}/assign")
    public ResponseEntity<Void> assignTicketToAdvisor(@PathVariable Long advisorId) {
        log.info("Assigning ticket to advisor: {}", advisorId);
        advisorService.assignTicket(advisorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/advisors/{advisorId}/complete")
    public ResponseEntity<Void> completeTicket(@PathVariable Long advisorId) {
        log.info("Completing ticket for advisor: {}", advisorId);
        advisorService.completeTicket(advisorId);
        return ResponseEntity.ok().build();
    }
}