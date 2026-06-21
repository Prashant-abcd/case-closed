package com.caseclosed.backend.controller;

import com.caseclosed.backend.dto.session.SessionResponse;
import com.caseclosed.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * POST /api/session
     *
     * Called once when the user lands on the home page.
     * Creates an anonymous guest session and returns a JWT token
     * that authenticates all subsequent game requests.
     */
    @PostMapping
    public ResponseEntity<SessionResponse> createSession() {
        return ResponseEntity.ok(sessionService.createSession());
    }
}
