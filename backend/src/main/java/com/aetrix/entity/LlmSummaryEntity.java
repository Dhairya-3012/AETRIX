package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "llm_summaries", indexes = {
        @Index(name = "idx_llm_feature", columnList = "feature_key"),
        @Index(name = "idx_llm_city", columnList = "city")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"city", "feature_key"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", length = 50, nullable = false)
    @Builder.Default
    private String city = "Ahmedabad";

    @Column(name = "feature_key", length = 50, nullable = false)
    private String featureKey;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    @Column(name = "model_used", length = 50)
    private String modelUsed;

    @Column(name = "generated_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime generatedAt;

    @Column(name = "last_regenerated")
    private LocalDateTime lastRegenerated;
}
