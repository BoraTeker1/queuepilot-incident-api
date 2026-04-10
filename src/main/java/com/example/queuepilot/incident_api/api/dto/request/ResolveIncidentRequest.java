package com.example.queuepilot.incident_api.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResolveIncidentRequest {

    @NotBlank(message = "resolution summary is required")
    private String resolutionSummary;


}
