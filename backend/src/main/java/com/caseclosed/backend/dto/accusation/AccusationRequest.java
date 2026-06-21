package com.caseclosed.backend.dto.accusation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AccusationRequest {
    @NotNull(message = "Suspect ID must be provided")
    private UUID suspectId;
}
