package com.caseclosed.backend.controller;

import com.caseclosed.backend.dto.folder.FolderResponse;
import com.caseclosed.backend.dto.folder.UpdateFolderRequest;
import com.caseclosed.backend.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/suspects/{suspectId}/folder")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping
    public ResponseEntity<FolderResponse> getFolder(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId
    ) {
        return ResponseEntity.ok(folderService.getFolder(caseId, suspectId));
    }

    @PatchMapping
    public ResponseEntity<FolderResponse> updateFolder(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId,
            @Valid @RequestBody UpdateFolderRequest request
    ) {
        return ResponseEntity.ok(folderService.updateFolder(caseId, suspectId, request));
    }
}
