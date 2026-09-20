package com.hasfatempire.rumorshield.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hasfatempire.rumorshield.model.ClaimDataset;
import com.hasfatempire.rumorshield.model.ClaimRecord;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Loads claims-dataset.json from the classpath once at startup and holds it
 * in memory. This is deliberately NOT a database or vector store for the
 * POC — the dataset is small (a few dozen curated records), so an in-memory
 * list keeps the demo fast and dependency-free. Swap for Postgres later if
 * the dataset grows past a few hundred records.
 */
@Slf4j
@Service
public class ClaimDatasetService {

    private List<ClaimRecord> records;

    @PostConstruct
    public void loadDataset() {
        try (InputStream is = new ClassPathResource("claims-dataset.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            ClaimDataset dataset = mapper.readValue(is, ClaimDataset.class);
            this.records = dataset.getRecords();
            log.info("Loaded {} curated claim records", records.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load claims-dataset.json", e);
        }
    }

    public List<ClaimRecord> getAllRecords() {
        return records;
    }
}
