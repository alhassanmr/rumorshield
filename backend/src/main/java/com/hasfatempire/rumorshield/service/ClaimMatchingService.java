package com.hasfatempire.rumorshield.service;

import com.hasfatempire.rumorshield.model.ClaimRecord;
import com.hasfatempire.rumorshield.model.ExtractedClaim;
import com.hasfatempire.rumorshield.model.Verdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * THIS is the class that matters most for the hackathon's core claim:
 * "we don't let the LLM decide whether something is true."
 *
 * Matching and the verdict decision are plain, deterministic, testable Java —
 * no model call, no prompt, no hallucination risk. The LLM is used only
 * before this (to extract a structured claim from free text) and after this
 * (to explain the result in plain language). This class is the audit-able
 * core of the whole system, and the thing to point to if a judge asks
 * "how do you know the AI isn't just making up the verdict?"
 */
@Service
@RequiredArgsConstructor
public class ClaimMatchingService {

    private final ClaimDatasetService datasetService;

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    public record MatchResult(ClaimRecord record, Verdict verdict, String evidenceStrength) {
        public static MatchResult noMatch() {
            return new MatchResult(null, Verdict.UNVERIFIED, "Low");
        }
    }

    public MatchResult match(ExtractedClaim claim) {
        List<ClaimRecord> records = datasetService.getAllRecords();

        ClaimRecord best = null;
        int bestScore = 0;

        for (ClaimRecord record : records) {
            int score = score(claim, record);
            if (score > bestScore) {
                bestScore = score;
                best = record;
            }
        }

        // Require a minimum overlap before we're willing to claim a match at all.
        if (best == null || bestScore < 2) {
            return MatchResult.noMatch();
        }

        Verdict verdict = decideVerdict(claim, best);
        String strength = bestScore >= 4 ? "High" : (bestScore >= 3 ? "Medium" : "Low");

        return new MatchResult(best, verdict, strength);
    }

    private int score(ExtractedClaim claim, ClaimRecord record) {
        int score = 0;
        String claimTopic = safe(claim.getTopic());
        String claimSubject = safe(claim.getSubject());
        String recordTopic = safe(record.getTopic());
        String recordSubject = safe(record.getSubject());

        if (!claimTopic.isBlank() && recordTopic.contains(claimTopic)) score += 2;
        if (!claimSubject.isBlank() && containsAnyWord(recordSubject, claimSubject)) score += 1;
        if (!claimSubject.isBlank() && containsAnyWord(safe(record.getVerifiedFact()), claimSubject)) score += 1;

        // Bonus: shared keywords between raw claim text and record subject/verifiedFact
        String rawText = safe(claim.getRawText());
        for (String word : recordTopic.split("\\s+")) {
            if (word.length() > 3 && rawText.contains(word)) score += 1;
        }

        return score;
    }

    private Verdict decideVerdict(ExtractedClaim claim, ClaimRecord record) {
        String status = safe(record.getStatus());

        if ("contradicted".equals(status)) {
            return Verdict.CONTRADICTED;
        }
        if ("outdated_as_of_today".equals(status)) {
            return Verdict.OUTDATED;
        }

        // status == "reference": compare the claimed numeric value (if any)
        // against the verified fact's numeric value (if any).
        Optional<String> claimedNumber = extractNumber(claim.getClaimedValue());
        Optional<String> verifiedNumber = extractNumber(record.getVerifiedFact());

        if (claimedNumber.isPresent() && verifiedNumber.isPresent()) {
            return claimedNumber.get().equals(verifiedNumber.get())
                    ? Verdict.VERIFIED
                    : Verdict.CONTRADICTED;
        }

        // No comparable numbers extracted - we found a relevant record but
        // can't mechanically confirm agreement, so don't overclaim VERIFIED.
        return Verdict.UNVERIFIED;
    }

    private Optional<String> extractNumber(String text) {
        if (text == null) return Optional.empty();
        Matcher m = NUMBER.matcher(text.replace(",", ""));
        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }

    private boolean containsAnyWord(String haystack, String needlePhrase) {
        return Arrays.stream(needlePhrase.split("\\s+"))
                .filter(w -> w.length() > 3)
                .anyMatch(haystack::contains);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
