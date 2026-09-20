package com.hasfatempire.rumorshield.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaimDataset {
    private List<ClaimRecord> records;
}
