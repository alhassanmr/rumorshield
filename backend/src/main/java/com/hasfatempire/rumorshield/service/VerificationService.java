package com.hasfatempire.rumorshield.service;

import com.hasfatempire.rumorshield.model.*;
import com.hasfatempire.rumorshield.repository.VerificationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The full pipeline, in order:
 *
 *   raw text
 *     -> [code] InternalReasoningEngine.extractClaim  (rule-based, no external API)
 *     -> [code] ClaimMatchingService.match            <-- the verdict is decided HERE
 *     -> [code] build recommendation + evidence strength label
 *     -> [code] InternalReasoningEngine.explainVerdict (template-based, no external API)
 *     -> [db]   persist as VerificationResultEntity
 *     -> response
 *
 * Every step in this pipeline is deterministic Java - no LLM, no external
 * API call, no network dependency at request time. See InternalReasoningEngine
 * for the trade-offs of this approach vs. an LLM-based one.
 */
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final InternalReasoningEngine reasoningEngine;
    private final ClaimMatchingService matchingService;
    private final VerificationResultRepository repository;

    public VerifyResponse verify(VerifyRequest request) {
        String rawText = request.getClaimText().trim();

        ExtractedClaim extractedClaim = reasoningEngine.extractClaim(rawText);

        ClaimMatchingService.MatchResult match = matchingService.match(extractedClaim);

        String explanation = reasoningEngine.explainVerdict(extractedClaim, match.record(), match.verdict());

        String recommendation = buildRecommendation(match.verdict());
        List<VerifyResponse.EvidenceSource> sources = buildSources(match.record());
        String shortSummary = buildShortSummary(match.verdict(), match.record());
        String claimId = UUID.randomUUID().toString();

        VerifyResponse response = VerifyResponse.builder()
                .claimId(claimId)
                .originalText(rawText)
                .extractedClaim(extractedClaim)
                .verdict(match.verdict())
                .evidenceStrength(match.evidenceStrength())
                .explanation(explanation)
                .recommendation(recommendation)
                .sources(sources)
                .shortSummary(shortSummary)
                .build();

        persist(response, match.record());

        return response;
    }

    /** Used by GET /api/v1/claims/{id} to retrieve a past result. */
    public Optional<VerifyResponse> findById(String claimId) {
        return repository.findById(claimId).map(this::toResponse);
    }

    private void persist(VerifyResponse response, ClaimRecord matchedRecord) {
        VerificationResultEntity entity = new VerificationResultEntity();
        entity.setId(response.getClaimId());
        entity.setOriginalText(response.getOriginalText());
        entity.setExtractedTopic(response.getExtractedClaim().getTopic());
        entity.setExtractedSubject(response.getExtractedClaim().getSubject());
        entity.setExtractedClaimedValue(response.getExtractedClaim().getClaimedValue());
        entity.setVerdict(response.getVerdict());
        entity.setEvidenceStrength(response.getEvidenceStrength());
        entity.setExplanation(response.getExplanation());
        entity.setRecommendation(response.getRecommendation());
        entity.setShortSummary(response.getShortSummary());

        if (matchedRecord != null) {
            entity.setSourceName(matchedRecord.getSourceName());
            entity.setSourceUrl(matchedRecord.getSourceUrl());
            entity.setSourceType(matchedRecord.getSourceType());
            entity.setSourcePublishedDate(matchedRecord.getPublishedDate());
        }

        repository.save(entity);
    }

    private VerifyResponse toResponse(VerificationResultEntity entity) {
        ExtractedClaim extractedClaim = new ExtractedClaim();
        extractedClaim.setTopic(entity.getExtractedTopic());
        extractedClaim.setSubject(entity.getExtractedSubject());
        extractedClaim.setClaimedValue(entity.getExtractedClaimedValue());
        extractedClaim.setRawText(entity.getOriginalText());

        List<VerifyResponse.EvidenceSource> sources = entity.getSourceUrl() == null
                ? List.of()
                : List.of(VerifyResponse.EvidenceSource.builder()
                        .name(entity.getSourceName())
                        .url(entity.getSourceUrl())
                        .type(entity.getSourceType())
                        .publishedDate(entity.getSourcePublishedDate())
                        .build());

        return VerifyResponse.builder()
                .claimId(entity.getId())
                .originalText(entity.getOriginalText())
                .extractedClaim(extractedClaim)
                .verdict(entity.getVerdict())
                .evidenceStrength(entity.getEvidenceStrength())
                .explanation(entity.getExplanation())
                .recommendation(entity.getRecommendation())
                .sources(sources)
                .shortSummary(entity.getShortSummary())
                .build();
    }

    private String buildRecommendation(Verdict verdict) {
        return switch (verdict) {
            case VERIFIED -> "You can share this information — include the official source if you pass it on.";
            case CONTRADICTED -> "Do not share this claim as fact. The available evidence contradicts it.";
            case OUTDATED -> "This information was accurate before, but has since changed or expired. Check the current status before acting.";
            case UNVERIFIED -> "We could not find enough evidence in our curated sources. Don't treat this as confirmed — verify with the relevant official source first.";
        };
    }

    private List<VerifyResponse.EvidenceSource> buildSources(ClaimRecord record) {
        if (record == null) return List.of();
        return List.of(VerifyResponse.EvidenceSource.builder()
                .name(record.getSourceName())
                .url(record.getSourceUrl())
                .type(record.getSourceType())
                .publishedDate(record.getPublishedDate())
                .build());
    }

    private String buildShortSummary(Verdict verdict, ClaimRecord record) {
        String label = switch (verdict) {
            case VERIFIED -> "VERIFIED";
            case CONTRADICTED -> "CONTRADICTED";
            case OUTDATED -> "OUTDATED";
            case UNVERIFIED -> "UNVERIFIED";
        };
        String sourceNote = record != null ? " Source: " + record.getSourceName() + "." : " No matching source found.";
        String full = label + "." + sourceNote;
        return full.length() > 160 ? full.substring(0, 157) + "..." : full;
    }
}
