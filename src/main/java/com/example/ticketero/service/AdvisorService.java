package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.repository.AdvisorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvisorService {

    private final AdvisorRepository advisorRepository;

    public List<AdvisorResponse> findAvailable() {
        return advisorRepository.findByStatus(AdvisorStatus.AVAILABLE)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public Optional<Advisor> findBestAvailableAdvisor() {
        List<Advisor> available = advisorRepository.findAvailableOrderByAssignedCount();
        return available.isEmpty() ? Optional.empty() : Optional.of(available.get(0));
    }

    @Transactional
    public void assignTicket(Long advisorId) {
        Advisor advisor = advisorRepository.findById(advisorId)
            .orElseThrow(() -> new RuntimeException("Advisor not found"));
        
        advisor.setAssignedTicketsCount(advisor.getAssignedTicketsCount() + 1);
        advisor.setStatus(AdvisorStatus.BUSY);
        
        log.info("Assigned ticket to advisor: {}", advisor.getName());
    }

    @Transactional
    public void completeTicket(Long advisorId) {
        Advisor advisor = advisorRepository.findById(advisorId)
            .orElseThrow(() -> new RuntimeException("Advisor not found"));
        
        advisor.setAssignedTicketsCount(Math.max(0, advisor.getAssignedTicketsCount() - 1));
        advisor.setStatus(AdvisorStatus.AVAILABLE);
        
        log.info("Completed ticket for advisor: {}", advisor.getName());
    }

    private AdvisorResponse toResponse(Advisor advisor) {
        return new AdvisorResponse(
            advisor.getId(),
            advisor.getName(),
            advisor.getEmail(),
            advisor.getStatus().name(),
            advisor.getModuleNumber(),
            advisor.getAssignedTicketsCount(),
            advisor.getCreatedAt()
        );
    }
}