package com.caseclosed.backend.dto.cases;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvidenceSearchResponse {
    private boolean matchFound;
    private boolean alreadyFound;
    private EvidenceResponse evidence;
    private int strikeCount;
    private boolean gameOver;
}
