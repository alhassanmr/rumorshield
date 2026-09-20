package com.hasfatempire.rumorshield.model;

/**
 * The four statuses RumorShield can return. Deliberately not a simple
 * true/false — see hackathon brief section on why binary verdicts are
 * misleading for real-world civic claims.
 */
public enum Verdict {
    VERIFIED,       // matches an official/reference record, currently accurate
    CONTRADICTED,   // matches a record, but the claim disagrees with it (includes known hoaxes)
    OUTDATED,       // matches a record that was true once but has since expired/changed
    UNVERIFIED      // no matching record found in the curated dataset
}
