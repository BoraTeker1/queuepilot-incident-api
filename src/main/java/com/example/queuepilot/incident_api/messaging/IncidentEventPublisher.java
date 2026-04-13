package com.example.queuepilot.incident_api.messaging;

import com.example.queuepilot.incident_api.domain.entity.Incident;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncidentEventPublisher {

    private static final String INCIDENT_CREATED_TOPIC = "incident-created";

    private final KafkaTemplate<String, IncidentCreatedEvent> kafkaTemplate;

    public IncidentEventPublisher(KafkaTemplate<String, IncidentCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishIncidentCreated(Incident incident) {
        IncidentCreatedEvent event = new IncidentCreatedEvent(
                incident.getId(),
                incident.getService().getId(),
                incident.getService().getServiceName(),
                incident.getSeverity().name(),
                incident.getSource().name(),
                incident.getDedupeKey(),
                incident.getCreatedAt().toString()
        );

        kafkaTemplate.send(INCIDENT_CREATED_TOPIC, incident.getId().toString(), event);
    }
}