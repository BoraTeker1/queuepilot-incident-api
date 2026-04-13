package com.example.queuepilot.incident_api.api.controller;

import com.example.queuepilot.incident_api.api.dto.request.AssignIncidentRequest;
import com.example.queuepilot.incident_api.api.dto.request.CreateIncidentRequest;
import com.example.queuepilot.incident_api.api.dto.request.ResolveIncidentRequest;
import com.example.queuepilot.incident_api.api.dto.response.IncidentResponse;
import com.example.queuepilot.incident_api.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService incidentService;

    @Autowired
    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public IncidentResponse createIncident(@RequestBody @Valid CreateIncidentRequest request) {
        return incidentService.createIncident(request);
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        return incidentService.getIncidentById(id);
    }

    @GetMapping
    public  List<IncidentResponse> getAllIncidents() {
        return incidentService.getAllIncidents();
    }


    @PatchMapping("/{id}/start")
    public IncidentResponse startIncident(@PathVariable Long id) {
        return incidentService.startIncident(id);
    }

    @PatchMapping("/{id}/close")
    public IncidentResponse closeIncident(@PathVariable Long id) {
        return incidentService.closeIncident(id);
    }

    @PatchMapping("/{id}/acknowledge")
    public IncidentResponse acknowledgeIncident(@PathVariable Long id) {
        return incidentService.acknowledgeIncident(id);
    }
    @PatchMapping("/{id}/assign")
    public IncidentResponse assignIncident(@PathVariable Long id,
                                           @RequestBody @Valid AssignIncidentRequest request) {
        return incidentService.assignIncident(id, request);
    }

    @PatchMapping("/{id}/resolve")
    public IncidentResponse resolveIncident(@PathVariable Long id, @RequestBody @Valid ResolveIncidentRequest request) {
        return incidentService.resolveIncident(id, request);
    }
}
