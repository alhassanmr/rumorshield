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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimMatchingService {

    private final ClaimDatasetService datasetService;

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    private static final Pattern CURRENCY_NUMBER = Pattern.compile(
            "(?:GH[SC¢₵]|cedis?)\\s*(\\d[\\d,]*(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

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

        String rawText = safe(claim.getRawText());
        for (String word : recordTopic.split("\\s+")) {
            if (word.length() > 3 && rawText.contains(word)) score += 1;
        }

        Set<String> topicWords = Arrays.stream(recordTopic.split("\\s+"))
                .map(w -> w.replaceAll("[^a-z0-9]", ""))
                .collect(Collectors.toSet());
        for (String word : recordSubject.split("\\s+")) {
            String cleaned = word.replaceAll("[^a-z0-9]", "");
            if (cleaned.length() > 4 && !topicWords.contains(cleaned) && rawText.contains(cleaned)) {
                score += 1;
            }
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

        Optional<String> claimedNumber = extractNumber(claim.getClaimedValue());
        Optional<String> verifiedNumber = extractCurrencyNumber(record.getVerifiedFact());

        if (claimedNumber.isPresent() && verifiedNumber.isPresent()) {
            return claimedNumber.get().equals(verifiedNumber.get())
                    ? Verdict.VERIFIED
                    : Verdict.CONTRADICTED;
        }

        return Verdict.UNVERIFIED;
    }

    private Optional<String> extractNumber(String text) {
        if (text == null) return Optional.empty();
        Matcher m = NUMBER.matcher(text.replace(",", ""));
        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }

    private Optional<String> extractCurrencyNumber(String text) {
        if (text == null) return Optional.empty();
        Matcher m = CURRENCY_NUMBER.matcher(text);
        return m.find() ? Optional.of(m.group(1).replace(",", "")) : Optional.empty();
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
