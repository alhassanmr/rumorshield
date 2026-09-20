package com.hasfatempire.rumorshield.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Structured output of "LLM Job 1": turning messy free text into something
 * we can match against the curated dataset. The LLM's role stops here —
 * it does NOT decide true/false, it only extracts what the claim is about.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedClaim {
    private String topic;          // e.g. "Ghana Card", "NHIS", "Passport"
    private String subject;        // e.g. "renewal fee"
    private String claimedValue;   // e.g. "200" or "free" — what the text asserts
    private String rawText;        // original text, kept for the audit trail
}
