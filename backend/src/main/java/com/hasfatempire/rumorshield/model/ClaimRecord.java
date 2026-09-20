package com.hasfatempire.rumorshield.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * One entry in the curated local knowledge base (claims-dataset.json).
 * This is the "ground truth" RumorShield matches incoming claims against.
 * Nothing here is fetched live — it is a deliberately scoped, human-curated
 * set of Ghanaian civic facts and known hoaxes, each with a real source.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaimRecord {
    private String id;
    private String topic;
    private String subject;
    private String verifiedFact;
    private List<String> relatedFacts;
    private String sourceName;
    private String sourceUrl;
    private String sourceType;      // official | news | factcheck
    private String publishedDate;
    private String effectiveDate;
    private String expiryDate;
    private String status;          // reference | outdated_as_of_today | contradicted
    private String notes;
}
