package com.example.queuepilot.incident_api.api.dto.response;


import com.example.queuepilot.incident_api.domain.enums.IncidentSource;
import com.example.queuepilot.incident_api.domain.enums.IncidentStatus;
import com.example.queuepilot.incident_api.domain.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class IncidentResponse {
    private Long id;
    private Long serviceId;
    private String serviceName;
    private String title;
    private String description;
    private Severity severity;
    private IncidentSource source;
    private IncidentStatus incidentStatus;
    private Integer priorityScore;
    private String dedupeKey;
    private Long assignedToUserId;
    private String assignedToUserName;
    private Long createdByUserId;
    private String createdByUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
}
