package com.caseclosed.backend.dto.cases;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresentEvidenceResponse {
    private boolean success;
    private String npcMessage;
    private int strikeCount;
    private boolean gameOver;
}
