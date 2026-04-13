package com.example.queuepilot.incident_api.service;


import com.example.queuepilot.incident_api.api.dto.request.AssignIncidentRequest;
import com.example.queuepilot.incident_api.api.dto.request.CreateIncidentRequest;
import com.example.queuepilot.incident_api.api.dto.request.ResolveIncidentRequest;
import com.example.queuepilot.incident_api.api.dto.response.IncidentResponse;
import com.example.queuepilot.incident_api.domain.entity.Incident;
import com.example.queuepilot.incident_api.domain.entity.IncidentEvent;
import com.example.queuepilot.incident_api.domain.entity.ServiceEntity;
import com.example.queuepilot.incident_api.domain.entity.User;
import com.example.queuepilot.incident_api.domain.enums.ActorType;
import com.example.queuepilot.incident_api.domain.enums.EventType;
import com.example.queuepilot.incident_api.domain.enums.IncidentStatus;
import com.example.queuepilot.incident_api.exception.DuplicateResourceException;
import com.example.queuepilot.incident_api.exception.InvalidStateException;
import com.example.queuepilot.incident_api.exception.ResourceNotFoundException;
import com.example.queuepilot.incident_api.messaging.IncidentCreatedEvent;
import com.example.queuepilot.incident_api.messaging.IncidentEventPublisher;
import com.example.queuepilot.incident_api.repository.IncidentEventRepository;
import com.example.queuepilot.incident_api.repository.IncidentRepository;
import com.example.queuepilot.incident_api.repository.ServiceEntityRepository;
import com.example.queuepilot.incident_api.repository.UserRepository;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final ServiceEntityRepository serviceEntityRepository;

    private final UserRepository userRepository;

    private final IncidentEventPublisher incidentEventPublisher;

    public IncidentService(IncidentRepository incidentRepository, IncidentEventRepository incidentEventRepository, ServiceEntityRepository serviceEntityRepository, UserRepository userRepository, IncidentEventPublisher incidentEventPublisher) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.serviceEntityRepository = serviceEntityRepository;
        this.userRepository = userRepository;
        this.incidentEventPublisher = incidentEventPublisher;
    }

    private void validateStatusTransition(IncidentStatus current, IncidentStatus target) {
        boolean isValid =
                (current == IncidentStatus.OPEN && target == IncidentStatus.ACKNOWLEDGED) ||
                        (current == IncidentStatus.ACKNOWLEDGED && target == IncidentStatus.IN_PROGRESS) ||
                        (current == IncidentStatus.IN_PROGRESS && target == IncidentStatus.RESOLVED) ||
                        (current == IncidentStatus.ACKNOWLEDGED && target == IncidentStatus.RESOLVED) ||
                        (current == IncidentStatus.RESOLVED && target == IncidentStatus.CLOSED);

        if (!isValid) {
            throw new InvalidStateException(
                    "Invalid status transition from " + current + " to " + target);
        }
    }

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        Long serviceId = request.getServiceId();
        ServiceEntity service = serviceEntityRepository.findById(serviceId).
                orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        if (request.getDedupeKey() != null && !request.getDedupeKey().isBlank()) {
            if (incidentRepository.findByDedupeKey(request.getDedupeKey()).isPresent()) {
                throw new DuplicateResourceException("Incident already exists with that dedupe key: " + request.getDedupeKey());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        Incident incident = new Incident();
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setSeverity(request.getSeverity());
        incident.setSource(request.getSource());
        incident.setDedupeKey(request.getDedupeKey());
        incident.setService(service);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedAt(now);
        incident.setUpdatedAt(now);

        Incident savedIncident= incidentRepository.save(incident);

        IncidentEvent incidentEvent = new IncidentEvent();
        incidentEvent.setIncident(savedIncident);
        incidentEvent.setEventType(EventType.CREATED);
        incidentEvent.setActorType(ActorType.SYSTEM);
        incidentEvent.setActorId(null);
        incidentEvent.setDetails("Incident created");
        incidentEvent.setCreatedAt(now);

        incidentEventRepository.save(incidentEvent);
        incidentEventPublisher.publishIncidentCreated(savedIncident);

        return mapToResponse(savedIncident);

    }

    public IncidentResponse getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("incident with the following id not found " + id));

        return mapToResponse(incident);
    }

    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));

        validateStatusTransition(incident.getStatus(), IncidentStatus.ACKNOWLEDGED);
        LocalDateTime now = LocalDateTime.now();
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(now);
        incident.setUpdatedAt(now);

        Incident savedIncident = incidentRepository.save(incident);

        IncidentEvent incidentEvent = new IncidentEvent();

        incidentEvent.setIncident(savedIncident);
        incidentEvent.setEventType(EventType.ACKNOWLEDGED);
        incidentEvent.setActorType(ActorType.SYSTEM);
        incidentEvent.setActorId(null);
        incidentEvent.setDetails("Incident acknowledged");
        incidentEvent.setCreatedAt(now);

        incidentEventRepository.save(incidentEvent);

        return mapToResponse(savedIncident);
    }

    @Transactional
    public IncidentResponse startIncident(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with the id: " + incidentId));

        validateStatusTransition(incident.getStatus(), IncidentStatus.IN_PROGRESS);
        LocalDateTime now = LocalDateTime.now();
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setUpdatedAt(now);

        Incident savedIncident = incidentRepository.save(incident);
        IncidentEvent incidentEvent = new IncidentEvent();
        incidentEvent.setIncident(savedIncident);
        incidentEvent.setEventType(EventType.STATUS_CHANGED);
        incidentEvent.setActorType(ActorType.SYSTEM);
        incidentEvent.setActorId(null);
        incidentEvent.setDetails("Incident moved to IN_PROGRESS");
        incidentEvent.setCreatedAt(now);

        incidentEventRepository.save(incidentEvent);

        return mapToResponse(savedIncident);
    }

    @Transactional
    public IncidentResponse closeIncident(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with the id: " + incidentId));
        validateStatusTransition(incident.getStatus(), IncidentStatus.CLOSED);
        LocalDateTime now = LocalDateTime.now();

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setUpdatedAt(now);

        Incident savedIncident = incidentRepository.save(incident);

        IncidentEvent incidentEvent = new IncidentEvent();
        incidentEvent.setIncident(savedIncident);
        incidentEvent.setEventType(EventType.STATUS_CHANGED);
        incidentEvent.setActorType(ActorType.SYSTEM);
        incidentEvent.setActorId(null);
        incidentEvent.setDetails("Incident closed");
        incidentEvent.setCreatedAt(now);

        incidentEventRepository.save(incidentEvent);
        return mapToResponse(savedIncident);
    }


    @Transactional
    public IncidentResponse assignIncident(Long incidentId, AssignIncidentRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with the id: " + incidentId));


        if (incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.CLOSED) {
            throw new InvalidStateException("Resolved or closed incidents cannot be assigned");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id:" + request.getUserId()));
        LocalDateTime now = LocalDateTime.now();

        incident.setAssignedTo(user);
        incident.setUpdatedAt(now);

        Incident savedIncident = incidentRepository.save(incident);

        IncidentEvent incidentEvent = new IncidentEvent();
        incidentEvent.setIncident(savedIncident);
        incidentEvent.setEventType(EventType.ASSIGNED);
        incidentEvent.setActorType(ActorType.SYSTEM);
        incidentEvent.setActorId(null);
        incidentEvent.setDetails("Incident assigned to user id: " + user.getId());
        incidentEvent.setCreatedAt(now);

        incidentEventRepository.save(incidentEvent);
        return mapToResponse(savedIncident);

    }

    @Transactional
    public IncidentResponse resolveIncident(Long incidentId, ResolveIncidentRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + incidentId));

        validateStatusTransition(incident.getStatus(), IncidentStatus.RESOLVED);

        LocalDateTime now = LocalDateTime.now();

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(now);
        incident.setUpdatedAt(now);

        Incident savedIncident = incidentRepository.save(incident);

        IncidentEvent incidentEvent = new IncidentEvent();
        incidentEvent.setIncident(savedIncident);
        incidentEvent.setEventType(EventType.RESOLVED);
        incidentEvent.setActorType(ActorType.SYSTEM);
        incidentEvent.setActorId(null);
        incidentEvent.setDetails("Incident resolved: " + request.getResolutionSummary());
        incidentEvent.setCreatedAt(now);

        incidentEventRepository.save(incidentEvent);

        return mapToResponse(savedIncident);
    }


    private IncidentResponse mapToResponse(Incident incident) {
        IncidentResponse response = new IncidentResponse();
        response.setId(incident.getId());
        response.setTitle(incident.getTitle());
        response.setDescription(incident.getDescription());
        response.setSeverity(incident.getSeverity());
        response.setSource(incident.getSource());
        response.setIncidentStatus(incident.getStatus());
        response.setPriorityScore(incident.getPriorityScore());
        response.setDedupeKey(incident.getDedupeKey());
        response.setCreatedAt(incident.getCreatedAt());
        response.setUpdatedAt(incident.getUpdatedAt());
        response.setAcknowledgedAt(incident.getAcknowledgedAt());
        response.setResolvedAt(incident.getResolvedAt());

        if (incident.getService() != null) {
            response.setServiceId(incident.getService().getId());
            response.setServiceName(incident.getService().getServiceName());
        }
        if (incident.getAssignedTo() != null) {
            response.setAssignedToUserId(incident.getAssignedTo().getId());
            response.setAssignedToUserName(incident.getAssignedTo().getName());
        }
        if (incident.getCreatedBy() != null) {
            response.setCreatedByUserId(incident.getCreatedBy().getId());
            response.setCreatedByUserName(incident.getCreatedBy().getName());
        }
        return response;
        }







}
