package com.hasfatempire.rumorshield.repository;

import com.hasfatempire.rumorshield.model.VerificationResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationResultRepository extends JpaRepository<VerificationResultEntity, String> {
}
