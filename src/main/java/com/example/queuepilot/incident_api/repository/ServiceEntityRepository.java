package com.example.queuepilot.incident_api.repository;


import com.example.queuepilot.incident_api.domain.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceEntityRepository extends JpaRepository<ServiceEntity, Long> {
    Optional<ServiceEntity> findByServiceName(String serviceName);
}
