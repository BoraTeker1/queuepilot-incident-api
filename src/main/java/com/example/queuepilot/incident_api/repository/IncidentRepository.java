package com.example.queuepilot.incident_api.repository;

import com.example.queuepilot.incident_api.domain.entity.Incident;
import com.example.queuepilot.incident_api.domain.entity.ServiceEntity;
import com.example.queuepilot.incident_api.domain.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findByDedupeKey(String dedupeKey);
    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByService(ServiceEntity service);

    List<Incident> findByStatusAndService(IncidentStatus status, ServiceEntity service);

}
