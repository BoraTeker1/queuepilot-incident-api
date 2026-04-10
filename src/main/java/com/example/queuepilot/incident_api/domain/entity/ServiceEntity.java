package com.example.queuepilot.incident_api.domain.entity;


import com.example.queuepilot.incident_api.domain.enums.ServiceTier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name="services")
public class ServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String serviceName;

    @Column(nullable = false)
    private String ownerTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceTier tier;

    @Column(nullable = false)
    private Integer slaMinutes;

    @Column(nullable = false)
    private String runbookUrl;


}
