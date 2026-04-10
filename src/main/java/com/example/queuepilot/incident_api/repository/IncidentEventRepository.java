package com.example.queuepilot.incident_api.repository;

import com.example.queuepilot.incident_api.domain.entity.Incident;
import com.example.queuepilot.incident_api.domain.entity.IncidentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentEventRepository extends JpaRepository<IncidentEvent, Long> {
    List<IncidentEvent> findByIncidentOrderByCreatedAtAsc(Incident incident);



}
