package com.hasfatempire.rumorshield.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResponse {
    private String claimId;              // generated id for this verification request
    private String originalText;
    private ExtractedClaim extractedClaim;
    private Verdict verdict;
    private String evidenceStrength;     // "High" | "Medium" | "Low"
    private String explanation;          // plain-language, from LLM
    private String recommendation;       // "what you should do" line
    private List<EvidenceSource> sources;
    private String shortSummary;         // USSD-length (<=160 char) version

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceSource {
        private String name;
        private String url;
        private String type;       // official | news | factcheck
        private String publishedDate;
    }
}
