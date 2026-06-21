package com.caseclosed.backend.dto.session;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SessionResponse {
    private String token;
    private UUID sessionId;
}
