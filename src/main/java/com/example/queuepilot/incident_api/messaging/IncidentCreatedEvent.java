package com.example.queuepilot.incident_api.messaging;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCreatedEvent {
    private Long incidentId;
    private Long serviceId;
    private String serviceName;
    private String severity;
    private String source;
    private String dedupeKey;
    private String createdAt;
}
