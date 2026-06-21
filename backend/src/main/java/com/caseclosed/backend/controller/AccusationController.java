package com.caseclosed.backend.controller;

import com.caseclosed.backend.dto.accusation.AccusationRequest;
import com.caseclosed.backend.dto.accusation.VerdictResponse;
import com.caseclosed.backend.service.AccusationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/accuse")
@RequiredArgsConstructor
public class AccusationController {

    private final AccusationService accusationService;

    @PostMapping
    public ResponseEntity<VerdictResponse> accuseSuspect(
            @PathVariable UUID caseId,
            @Valid @RequestBody AccusationRequest request
    ) {
        return ResponseEntity.ok(accusationService.accuse(caseId, request));
    }
}
