package com.caseclosed.backend.dto.generation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedCaseData {
    private Map<String, Object> caseBriefing;
    private GeneratedSuspectData suspect;
    private List<GeneratedEvidenceData> evidence;
    private Map<String, Object> truthDocument;
}
