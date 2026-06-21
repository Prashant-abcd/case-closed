package com.caseclosed.backend.service;

import com.caseclosed.backend.dto.session.SessionResponse;
import com.caseclosed.backend.entity.GuestSession;
import com.caseclosed.backend.repository.GuestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final GuestSessionRepository guestSessionRepository;
    private final JwtService jwtService;

    @Transactional
    public SessionResponse createSession() {
        GuestSession session = guestSessionRepository.save(GuestSession.builder().build());
        String token = jwtService.generateToken(session.getId().toString());
        return SessionResponse.builder()
                .token(token)
                .sessionId(session.getId())
                .build();
    }
}
