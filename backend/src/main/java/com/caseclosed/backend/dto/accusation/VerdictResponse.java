package com.caseclosed.backend.dto.accusation;

import com.caseclosed.backend.entity.GameCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class VerdictResponse {
    private UUID caseId;
    @JsonProperty("isVictory")
    private boolean isVictory;
    private int score;
    private String narrative;
    private Map<String, Object> truthDocument;

    public static VerdictResponse from(GameCase gameCase) {
        // Parse the JSON verdict returned from Claude
        Map<String, Object> verdict = gameCase.getVerdict();
        boolean isVictory = (boolean) verdict.getOrDefault("isVictory", false);
        int score = ((Number) verdict.getOrDefault("score", 0)).intValue();
        String narrative = (String) verdict.getOrDefault("narrative", "The case was closed.");

        return VerdictResponse.builder()
                .caseId(gameCase.getId())
                .isVictory(isVictory)
                .score(score)
                .narrative(narrative)
                .truthDocument(gameCase.getTruthDocument())
                .build();
    }
}
