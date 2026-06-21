package com.caseclosed.backend.dto.cases;

import com.caseclosed.backend.entity.GameCase;
import com.caseclosed.backend.enums.CaseStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class CaseResponse {

    private UUID id;
    private CaseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Map<String, Object> caseBriefing;
    private List<SuspectResponse> suspects;
    private UUID accusationSuspectId;
    private Map<String, Object> verdict;
    private int strikeCount;

    public static CaseResponse from(GameCase gameCase, List<SuspectResponse> suspects) {
        return CaseResponse.builder()
                .id(gameCase.getId())
                .status(gameCase.getStatus())
                .createdAt(gameCase.getCreatedAt())
                .completedAt(gameCase.getCompletedAt())
                .caseBriefing(gameCase.getCaseBriefing())
                .suspects(suspects)
                .accusationSuspectId(gameCase.getAccusationSuspectId())
                .verdict(gameCase.getVerdict())
                .strikeCount(gameCase.getStrikeCount())
                .build();
    }
}
