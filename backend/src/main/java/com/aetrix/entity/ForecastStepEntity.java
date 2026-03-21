package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "forecast_steps", indexes = {
        @Index(name = "idx_forecast_type", columnList = "step_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step")
    private Integer step;

    @Column(name = "value_celsius")
    private Double valueCelsius;

    @Column(name = "step_type", length = 20)
    private String stepType;

    @Column(name = "predicted_celsius")
    private Double predictedCelsius;

    @Column(name = "lower_bound")
    private Double lowerBound;

    @Column(name = "upper_bound")
    private Double upperBound;

    @Column(name = "ingested_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
}
