package com.hasfatempire.rumorshield.service;

import com.hasfatempire.rumorshield.model.ClaimRecord;
import com.hasfatempire.rumorshield.model.ExtractedClaim;
import com.hasfatempire.rumorshield.model.Verdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InternalReasoningEngine {

    private final ClaimDatasetService datasetService;

    private static final Pattern AMOUNT = Pattern.compile(
            "(?:GH[SC¢₵]|cedis?)\\s*(\\d[\\d,]*(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    private static final List<String> SUBJECT_KEYWORDS = List.of(
            "renewal", "replacement", "registration", "fee", "cost", "grant",
            "promo", "free", "closed", "lost", "shut down", "expedited", "24-hour"
    );

    private static final java.util.Map<String, String> TOPIC_PORTALS = java.util.Map.of(
            "Ghana Card", "https://nia.gov.gh",
            "NHIS", "https://nhis.gov.gh",
            "Passport", "https://mfa.gov.gh",
            "Ghana Police", "https://police.gov.gh",
            "Government payments", "https://mofep.gov.gh",
            "Foreign policy", "https://mfa.gov.gh"
    );

    public ExtractedClaim extractClaim(String rawText) {
        String lower = rawText.toLowerCase(Locale.ROOT);

        String topic = detectTopic(lower);
        String subject = detectSubject(lower);
        String claimedValue = detectClaimedValue(lower);

        ExtractedClaim claim = new ExtractedClaim();
        claim.setTopic(topic);
        claim.setSubject(subject);
        claim.setClaimedValue(claimedValue);
        claim.setRawText(lower);
        return claim;
    }

    private String detectTopic(String lowerText) {
        return datasetService.getAllRecords().stream()
                .map(ClaimRecord::getTopic)
                .distinct()
                .filter(topic -> lowerText.contains(topic.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse("");
    }

    private String detectSubject(String lowerText) {
        return SUBJECT_KEYWORDS.stream()
                .filter(lowerText::contains)
                .findFirst()
                .orElse("");
    }

    private String detectClaimedValue(String lowerText) {
        if (lowerText.contains("free")) {
            return "free";
        }
        Matcher m = AMOUNT.matcher(lowerText);
        if (m.find()) {
            return m.group(1).replace(",", "");
        }
        return "";
    }

    public String explainVerdict(ExtractedClaim claim, ClaimRecord record, Verdict verdict) {
        if (record == null) {
            return buildNoMatchExplanation(claim);
        }

        return switch (verdict) {
            case VERIFIED -> "This matches official information. According to %s (%s): %s".formatted(
                    record.getSourceName(), niceDate(record.getPublishedDate()), record.getVerifiedFact());

            case CONTRADICTED -> "contradicted".equals(record.getStatus())
                    ? "This claim has been checked and found false. %s (%s) reports: %s".formatted(
                            record.getSourceName(), niceDate(record.getPublishedDate()), record.getVerifiedFact())
                    : "The value in this claim does not match verified information. According to %s (%s), the correct information is: %s".formatted(
                            record.getSourceName(), niceDate(record.getPublishedDate()), record.getVerifiedFact());

            case OUTDATED -> "This was accurate before, but is outdated now. %s (%s): %s".formatted(
                    record.getSourceName(), niceDate(record.getPublishedDate()), record.getVerifiedFact());

            case UNVERIFIED -> "We found a related record (%s from %s) but could not automatically "
                    .formatted(record.getSubject(), record.getSourceName())
                    + "confirm whether it supports or contradicts the specific figure in your claim. "
                    + "Check the source directly: " + record.getSourceUrl();
        };
    }

    private String niceDate(String isoDate) {
        return isoDate == null ? "date unknown" : isoDate;
    }

    private String buildNoMatchExplanation(ExtractedClaim claim) {
        String topic = claim.getTopic();
        String portal = (topic != null && !topic.isBlank()) ? TOPIC_PORTALS.get(topic) : null;

        if (portal != null) {
            return ("We recognize this as a %s-related claim, but don't have this specific one "
                    + "in our curated sources yet. That doesn't mean it's false or true - we simply "
                    + "haven't verified it. For authoritative information on %s, check the official "
                    + "source directly: %s").formatted(topic, topic, portal);
        }

        return "We checked this claim against our curated set of verified Ghanaian civic sources "
                + "and found no matching topic or record. That does not mean the claim is false - "
                + "it means this is outside what we've verified so far. Treat it as unconfirmed "
                + "until you can check an official source.";
    }
}
