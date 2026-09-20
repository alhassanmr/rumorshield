package com.hasfatempire.rumorshield.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyRequest {
    @NotBlank(message = "claimText must not be empty")
    @Size(max = 2000, message = "claimText must be under 2000 characters")
    private String claimText;

    /** Optional: "web" | "ussd" | "sms" - lets the frontend request a shorter response for USSD */
    private String channel = "web";
}
