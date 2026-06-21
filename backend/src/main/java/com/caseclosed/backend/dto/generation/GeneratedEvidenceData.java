package com.caseclosed.backend.dto.generation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedEvidenceData {
    private String name;
    private String description;
}
