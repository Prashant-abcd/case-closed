package com.caseclosed.backend.dto.cases;

import com.caseclosed.backend.entity.Evidence;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class EvidenceResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean isFound;
    private boolean isPresented;

    public static EvidenceResponse from(Evidence evidence) {
        return EvidenceResponse.builder()
                .id(evidence.getId())
                .name(evidence.getName())
                .description(evidence.getDescription())
                .isFound(evidence.isFound())
                .isPresented(evidence.isPresented())
                .build();
    }
}
