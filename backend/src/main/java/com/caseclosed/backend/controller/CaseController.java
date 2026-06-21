package com.caseclosed.backend.controller;

import com.caseclosed.backend.dto.cases.CaseResponse;
import com.caseclosed.backend.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    public ResponseEntity<CaseResponse> createCase() {
        return ResponseEntity.ok(caseService.createCase());
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseResponse> getCase(@PathVariable UUID caseId) {
        return ResponseEntity.ok(caseService.getCase(caseId));
    }
}
