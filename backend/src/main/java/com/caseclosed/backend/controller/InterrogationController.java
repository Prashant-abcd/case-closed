package com.caseclosed.backend.controller;

import com.caseclosed.backend.dto.interrogation.ConversationResponse;
import com.caseclosed.backend.dto.interrogation.InterrogationRequest;
import com.caseclosed.backend.service.InterrogationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/suspects/{suspectId}")
@RequiredArgsConstructor
public class InterrogationController {

    private final InterrogationService interrogationService;

    /**
     * POST /api/cases/{caseId}/suspects/{suspectId}/interrogate
     *
     * Sends the player's message to the suspect and streams Claude's
     * response back as Server-Sent Events (SSE).
     *
     * SSE Event types:
     *   - "token"  → each text chunk as Claude generates it
     *   - "done"   → the full completed response
     *   - "error"  → if something went wrong
     */
    @PostMapping(value = "/interrogate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter interrogate(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId,
            @Valid @RequestBody InterrogationRequest request
    ) {
        return interrogationService.interrogate(caseId, suspectId, request.getMessage());
    }

    /**
     * GET /api/cases/{caseId}/suspects/{suspectId}/conversation
     *
     * Returns the full conversation history for a suspect in this case.
     */
    @GetMapping("/conversation")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId
    ) {
        return ResponseEntity.ok(interrogationService.getConversation(caseId, suspectId));
    }

    /**
     * POST /api/cases/{caseId}/suspects/{suspectId}/end-interrogation
     *
     * Marks the interrogation session as ended.
     */
    @PostMapping("/end-interrogation")
    public ResponseEntity<Void> endInterrogation(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId
    ) {
        interrogationService.endInterrogation(caseId, suspectId);
        return ResponseEntity.ok().build();
    }
}
