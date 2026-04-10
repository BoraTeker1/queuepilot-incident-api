package com.example.queuepilot.incident_api.api.dto.request;

import com.example.queuepilot.incident_api.domain.enums.IncidentSource;
import com.example.queuepilot.incident_api.domain.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncidentRequest {

    @NotNull(message = "service id is required")
    private Long serviceId;

    @NotBlank(message = "title is required")
    private String title;
    private String description;

    @NotNull(message = "severity is required")
    private Severity severity;

    @NotNull(message = "source is required")
    private IncidentSource source;
    private String dedupeKey;
}
