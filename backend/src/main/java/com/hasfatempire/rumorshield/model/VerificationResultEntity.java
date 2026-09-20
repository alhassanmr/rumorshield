package com.hasfatempire.rumorshield.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persisted record of a single verification request. Deliberately flat
 * (one table) rather than the fully normalized Claim/Source/Evidence split
 * the product spec describes (section 20) - that level of normalization
 * only pays for itself once a claim can match multiple ranked evidence
 * items, which is out of scope for this sprint. Documented scope cut, not
 * an oversight - see backend/README.md.
 */
@Entity
@Table(name = "verification_results")
@Getter
@Setter
@NoArgsConstructor
public class VerificationResultEntity {

    @Id
    private String id; // same UUID used as claimId in the API response

    @Column(nullable = false, length = 2000)
    private String originalText;

    private String extractedTopic;
    private String extractedSubject;
    private String extractedClaimedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Verdict verdict;

    private String evidenceStrength;

    @Column(length = 2000)
    private String explanation;

    @Column(length = 1000)
    private String recommendation;

    @Column(length = 200)
    private String shortSummary;

    // Flattened single matched source - see class-level note on scope.
    private String sourceName;
    private String sourceUrl;
    private String sourceType;
    private String sourcePublishedDate;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
