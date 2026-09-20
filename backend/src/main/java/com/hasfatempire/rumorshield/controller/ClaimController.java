package com.hasfatempire.rumorshield.controller;

import com.hasfatempire.rumorshield.model.VerifyRequest;
import com.hasfatempire.rumorshield.model.VerifyResponse;
import com.hasfatempire.rumorshield.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClaimController {

    private final VerificationService verificationService;

    @PostMapping("/verify")
    public VerifyResponse verify(@Valid @RequestBody VerifyRequest request) {
        return verificationService.verify(request);
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<VerifyResponse> getById(@PathVariable String claimId) {
        return verificationService.findById(claimId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public String health() {
        return "RumorShield API is running";
    }
}
