package com.caseclosed.backend.dto.interrogation;

import com.caseclosed.backend.entity.Conversation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {

    private UUID conversationId;
    private UUID suspectId;
    private String suspectName;
    private List<Map<String, Object>> messages;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public static ConversationResponse from(Conversation conversation) {
        return ConversationResponse.builder()
                .conversationId(conversation.getId())
                .suspectId(conversation.getSuspect().getId())
                .suspectName(conversation.getSuspect().getName())
                .messages(conversation.getMessages())
                .startedAt(conversation.getStartedAt())
                .endedAt(conversation.getEndedAt())
                .build();
    }
}
