package com.caseclosed.backend.controller;

import com.caseclosed.backend.dto.cases.EvidenceResponse;
import com.caseclosed.backend.dto.cases.EvidenceSearchRequest;
import com.caseclosed.backend.dto.cases.EvidenceSearchResponse;
import com.caseclosed.backend.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/evidence")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService evidenceService;

    @GetMapping
    public ResponseEntity<List<EvidenceResponse>> getFoundEvidence(@PathVariable UUID caseId) {
        return ResponseEntity.ok(evidenceService.getFoundEvidence(caseId));
    }

    @PostMapping("/search")
    public ResponseEntity<EvidenceSearchResponse> searchEvidence(
            @PathVariable UUID caseId,
            @RequestBody EvidenceSearchRequest request) {
        return ResponseEntity.ok(evidenceService.searchEvidence(caseId, request));
    }

    @PostMapping("/{evidenceId}/present")
    public ResponseEntity<com.caseclosed.backend.dto.cases.PresentEvidenceResponse> presentEvidence(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId) {
        return ResponseEntity.ok(evidenceService.presentEvidence(caseId, evidenceId));
    }
}
