package com.example.queuepilot.incident_api.domain.entity;

import com.example.queuepilot.incident_api.domain.enums.IncidentSource;
import com.example.queuepilot.incident_api.domain.enums.IncidentStatus;
import com.example.queuepilot.incident_api.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name="incidents")
@Setter
@Getter
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSource source;

    private Integer priorityScore;

    @Column(unique = true)
    private String dedupeKey;

    @ManyToOne
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;


    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @ManyToOne(optional = false)
    @JoinColumn(name="service_id", nullable = false)
    private ServiceEntity service;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;


}
